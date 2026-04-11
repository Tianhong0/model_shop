package org.majun.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.service.BountyWebSocketService;
import org.majun.backend.vo.BountyMessageVO;
import org.majun.backend.websocket.BountyWebSocketSessionStore;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
@RequiredArgsConstructor
@Slf4j
/**
 * 悬赏消息 WebSocket 服务实现
 */
public class BountyWebSocketServiceImpl implements BountyWebSocketService, BountyWebSocketSessionStore {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, CopyOnWriteArraySet<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    @Override
    public void broadcastMessage(Long taskId, BountyMessageVO message) {
        if (taskId == null || message == null) {
            return;
        }
        Set<WebSocketSession> sessions = roomSessions.get(taskId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String text;
        try {
            var payload = new java.util.HashMap<String, Object>();
            payload.put("eventType", "NEW_MESSAGE");
            payload.put("taskId", taskId);
            payload.put("message", message);
            text = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.warn("序列化悬赏WS消息失败 taskId={}", taskId, ex);
            return;
        }

        TextMessage wsMessage = new TextMessage(text);
        for (WebSocketSession session : sessions) {
            if (session == null || !session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(wsMessage);
            } catch (Exception ex) {
                log.warn("推送悬赏WS消息失败 sessionId={}, taskId={}", session.getId(), taskId, ex);
            }
        }
    }

    @Override
    public void add(Long taskId, WebSocketSession session) {
        if (taskId == null || session == null) {
            return;
        }
        roomSessions.computeIfAbsent(taskId, key -> new CopyOnWriteArraySet<>()).add(session);
    }

    @Override
    public void remove(Long taskId, WebSocketSession session) {
        if (taskId == null || session == null) {
            return;
        }
        CopyOnWriteArraySet<WebSocketSession> sessions = roomSessions.get(taskId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            roomSessions.remove(taskId);
        }
    }
}
