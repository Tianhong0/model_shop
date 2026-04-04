package org.majun.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.entity.CsAdminStatus;
import org.majun.backend.entity.CsConversation;
import org.majun.backend.repository.CsAdminStatusRepository;
import org.majun.backend.repository.CsConversationRepository;
import org.majun.backend.vo.CsAdminStatusVO;
import org.majun.backend.vo.CsConversationVO;
import org.majun.backend.vo.CsMessageVO;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Service
public class CustomerServiceWebSocketService {

    private final ObjectMapper objectMapper;
    private final CsConversationRepository conversationRepository;
    private final CsAdminStatusRepository adminStatusRepository;

    public CustomerServiceWebSocketService(ObjectMapper objectMapper, CsConversationRepository conversationRepository,
                                         CsAdminStatusRepository adminStatusRepository) {
        this.objectMapper = objectMapper;
        this.conversationRepository = conversationRepository;
        this.adminStatusRepository = adminStatusRepository;
    }

    // 客服ID -> Session集合
    private final ConcurrentHashMap<Long, CopyOnWriteArraySet<WebSocketSession>> adminSessions = new ConcurrentHashMap<>();

    // 会话ID -> 用户Session集合（用于推送消息给用户端）
    private final ConcurrentHashMap<Long, CopyOnWriteArraySet<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    // 心跳节流：记录上次写库时间戳（毫秒），60秒内不重复写
    private final ConcurrentHashMap<Long, Long> lastHeartbeatWriteTime = new ConcurrentHashMap<>();
    private static final long HEARTBEAT_THROTTLE_MS = 60_000;

    // 注册用户Session（按会话ID）
    public void registerUserSession(Long conversationId, WebSocketSession session) {
        userSessions.computeIfAbsent(conversationId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("用户Session注册: conversationId={}, sessionCount={}", conversationId, userSessions.get(conversationId).size());
    }

    // 注销用户Session
    public void unregisterUserSession(Long conversationId, WebSocketSession session) {
        CopyOnWriteArraySet<WebSocketSession> sessions = userSessions.get(conversationId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                userSessions.remove(conversationId);
            }
        }
    }

    // 注册客服Session
    public void registerAdminSession(Long adminId, WebSocketSession session) {
        adminSessions.computeIfAbsent(adminId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("客服Session注册: adminId={}, sessionCount={}", adminId, adminSessions.get(adminId).size());
    }

    // 注销客服Session
    public void unregisterAdminSession(Long adminId, WebSocketSession session) {
        CopyOnWriteArraySet<WebSocketSession> sessions = adminSessions.get(adminId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                adminSessions.remove(adminId);
                lastHeartbeatWriteTime.remove(adminId);
            }
        }
    }

    // 更新客服心跳（节流：60秒内不重复写库）
    public void updateAdminHeartbeat(Long adminId) {
        long now = System.currentTimeMillis();
        Long lastWrite = lastHeartbeatWriteTime.get(adminId);
        if (lastWrite != null && (now - lastWrite) < HEARTBEAT_THROTTLE_MS) {
            return;
        }
        lastHeartbeatWriteTime.put(adminId, now);

        CsAdminStatus status = adminStatusRepository.findByAdminId(adminId);
        if (status != null) {
            status.setLastHeartbeat(LocalDateTime.now());
            status.setIsOnline(1);
            adminStatusRepository.updateById(status);
        }
    }

    // 推送新消息到会话
    public void pushNewMessage(Long conversationId, CsMessageVO message) {
        try {
            CsConversation conversation = conversationRepository.selectById(conversationId);
            if (conversation == null) return;

            String payload = objectMapper.writeValueAsString(Map.of(
                "eventType", "NEW_MESSAGE",
                "conversationId", conversationId,
                "message", message
            ));

            // 推送给用户
            if (conversation.getUserId() != null) {
                pushToConversationUsers(conversationId, payload);
            }

            // 推送给客服
            if (conversation.getAdminId() != null) {
                pushToAdmin(conversation.getAdminId(), payload);
            }
        } catch (Exception e) {
            log.error("推送新消息失败", e);
        }
    }

    // 推送会话状态更新
    public void pushConversationUpdate(CsConversationVO conversation) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "eventType", "CONVERSATION_UPDATE",
                "conversation", conversation
            ));

            // 推送给用户
            if (conversation.getUserId() != null) {
                pushToConversationUsers(conversation.getId(), payload);
            }

            // 推送给客服
            if (conversation.getAdminId() != null) {
                pushToAdmin(conversation.getAdminId(), payload);
            } else if (conversation.getStatus() != null && conversation.getStatus() == 0) {
                // 会话状态为 WAITING (0) 且没有客服接入，通知所有在线客服
                log.info("会话等待人工接入，通知所有在线客服: conversationId={}", conversation.getId());
                pushToAllOnlineAdmins(payload);
            }
        } catch (Exception e) {
            log.error("推送会话更新失败", e);
        }
    }

    /**
     * 推送会话分配/转接通知给指定客服
     * @param adminId 客服ID
     * @param conversation 会话信息
     * @param isAutoAssign 是否为自动分配
     */
    public void pushConversationAssigned(Long adminId, CsConversationVO conversation, boolean isAutoAssign) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "eventType", "CONVERSATION_ASSIGNED",
                "conversation", conversation,
                "isAutoAssign", isAutoAssign
            ));

            pushToAdmin(adminId, payload);
            log.info("推送会话分配通知给客服: adminId={}, conversationId={}, isAutoAssign={}",
                adminId, conversation.getId(), isAutoAssign);
        } catch (Exception e) {
            log.error("推送会话分配通知失败", e);
        }
    }

    // 推送给指定会话的用户端（同时清理已关闭的 session）
    private void pushToConversationUsers(Long conversationId, String payload) {
        CopyOnWriteArraySet<WebSocketSession> sessions = userSessions.get(conversationId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    log.warn("推送消息给用户失败: conversationId={}", conversationId, e);
                    sessions.remove(session);
                }
            } else {
                sessions.remove(session);
            }
        }
        if (sessions.isEmpty()) {
            userSessions.remove(conversationId);
        }
    }

    // 推送给指定客服（同时清理已关闭的 session）
    private void pushToAdmin(Long adminId, String payload) {
        CopyOnWriteArraySet<WebSocketSession> sessions = adminSessions.get(adminId);
        if (sessions == null || sessions.isEmpty()) {
            log.warn("推送消息给客服失败: adminId={}, 无在线session, 当前已注册adminIds={}", adminId, adminSessions.keySet());
            return;
        }

        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    log.warn("推送消息给客服失败: adminId={}", adminId, e);
                    sessions.remove(session);
                }
            } else {
                sessions.remove(session);
            }
        }
        if (sessions.isEmpty()) {
            adminSessions.remove(adminId);
        }
    }

    // 推送给所有在线客服（新会话通知）
    public void pushToAllOnlineAdmins(String payload) {
        List<Long> onlineAdminIds = adminStatusRepository.findOnlineAdminIds();
        log.info("推送消息给所有在线客服: onlineAdminIds={}, 已注册sessions={}", onlineAdminIds, adminSessions.keySet());

        if (onlineAdminIds == null || onlineAdminIds.isEmpty()) {
            log.warn("没有在线客服，无法推送消息");
            return;
        }

        int successCount = 0;
        for (Long adminId : onlineAdminIds) {
            CopyOnWriteArraySet<WebSocketSession> sessions = adminSessions.get(adminId);
            if (sessions != null && !sessions.isEmpty()) {
                pushToAdmin(adminId, payload);
                successCount++;
            } else {
                log.warn("客服 {} 在数据库中在线但没有 WebSocket session", adminId);
            }
        }
        log.info("成功推送给 {} 个在线客服", successCount);
    }

    // 获取在线客服列表
    public List<CsAdminStatusVO> getOnlineAdmins() {
        List<Long> onlineAdminIds = adminStatusRepository.findOnlineAdminIds();
        if (onlineAdminIds == null || onlineAdminIds.isEmpty()) {
            return List.of();
        }

        List<CsAdminStatus> statuses = adminStatusRepository.findByAdminIdIn(onlineAdminIds);
        return statuses.stream()
            .map(status -> CsAdminStatusVO.builder()
                .adminId(status.getAdminId())
                .adminNickname(status.getAdminNickname())
                .isOnline(status.getIsOnline() != null && status.getIsOnline() == 1)
                .currentConversationCount(status.getCurrentConversationCount())
                .totalServedCount(status.getTotalServedCount())
                .build())
            .collect(Collectors.toList());
    }
}
