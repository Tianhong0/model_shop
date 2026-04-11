package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.config.AiProperties;
import org.majun.backend.dto.CsMessageSendRequest;
import org.majun.backend.entity.CsAdminStatus;
import org.majun.backend.entity.CsConversation;
import org.majun.backend.entity.CsMessage;
import org.majun.backend.entity.SysUser;
import org.majun.backend.enums.CsConversationStatus;
import org.majun.backend.enums.CsMessageType;
import org.majun.backend.repository.CsAdminStatusRepository;
import org.majun.backend.repository.CsConversationRepository;
import org.majun.backend.repository.CsMessageRepository;
import org.majun.backend.repository.SysUserRepository;
import org.majun.backend.service.AiChatService;
import org.majun.backend.service.CustomerServiceService;
import org.majun.backend.service.CustomerServiceWebSocketService;
import org.majun.backend.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * 客服系统服务实现
 */
public class CustomerServiceServiceImpl implements CustomerServiceService {

    private final CsConversationRepository conversationRepository;
    private final CsMessageRepository messageRepository;
    private final CsAdminStatusRepository adminStatusRepository;
    private final SysUserRepository sysUserRepository;
    private final CustomerServiceWebSocketService webSocketService;
    private final AiProperties aiProperties;

    @Autowired(required = false)
    private AiChatService aiChatService;

    // ==================== 用户端 ====================

    @Override
    @Transactional
    public CsConversationVO startConversation(Long userId) {
        // 检查用户是否有进行中的会话
        CsConversation existingConversation = conversationRepository.findActiveByUserId(userId);
        if (existingConversation != null) {
            return convertToVO(existingConversation);
        }

        // 创建新会话
        CsConversation conversation = new CsConversation();
        conversation.setSessionNo("CS" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        conversation.setUserId(userId);

        // 获取用户信息
        SysUser user = sysUserRepository.selectById(userId);
        if (user != null) {
            conversation.setUserNickname(user.getNickname());
            conversation.setUserAvatar(user.getAvatar());
        }

        // 直接设置为进行中状态，由 AI 接管
        conversation.setStatus(CsConversationStatus.ACTIVE.getCode());
        conversation.setUnreadUserCount(0);
        conversation.setUnreadAdminCount(0);
        // adminId 为 null 表示 AI 模式

        conversationRepository.insert(conversation);

        // 发送 AI 欢迎消息
        if (aiProperties.getEnabled() && aiChatService != null) {
            final Long conversationId = conversation.getId();
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    sendAiWelcomeMessage(conversationId);
                } catch (Exception e) {
                    log.error("发送 AI 欢迎消息失败", e);
                }
            });
        }

        return convertToVO(conversation);
    }

    /**
     * 发送 AI 欢迎消息
     */
    private void sendAiWelcomeMessage(Long conversationId) {
        CsMessage welcomeMessage = new CsMessage();
        welcomeMessage.setConversationId(conversationId);
        welcomeMessage.setSenderId(-1L);
        welcomeMessage.setSenderRole("AI");
        welcomeMessage.setSenderNickname("智能助手");
        welcomeMessage.setMessageType(CsMessageType.TEXT.getCode());
        welcomeMessage.setContent("您好！我是智能客服助手 😊\n\n我可以帮您解答关于：\n• 3D打印价格咨询\n• 材料选择建议\n• 订单状态查询\n• 配送时间咨询\n\n请问有什么可以帮您的？\n\n如需人工客服，请发送人工客服或点击转人工按钮。");
        welcomeMessage.setIsRead(0);
        welcomeMessage.setCreateTime(LocalDateTime.now());
        messageRepository.insert(welcomeMessage);

        // 更新会话最后消息时间
        CsConversation conversation = conversationRepository.selectById(conversationId);
        if (conversation != null) {
            conversation.setLastMessageTime(LocalDateTime.now());
            conversationRepository.updateById(conversation);
        }

        // 推送欢迎消息给用户
        CsMessageVO messageVO = CsMessageVO.builder()
                .id(welcomeMessage.getId())
                .conversationId(welcomeMessage.getConversationId())
                .senderId(welcomeMessage.getSenderId())
                .senderRole(welcomeMessage.getSenderRole())
                .senderNickname(welcomeMessage.getSenderNickname())
                .messageType(welcomeMessage.getMessageType())
                .content(welcomeMessage.getContent())
                .isRead(false)
                .createTime(welcomeMessage.getCreateTime())
                .build();

        webSocketService.pushNewMessage(conversationId, messageVO);
    }

    @Override
    public CsConversationVO getActiveConversation(Long userId) {
        CsConversation conversation = conversationRepository.findActiveByUserId(userId);
        return conversation != null ? convertToVO(conversation) : null;
    }

    @Override
    public PageResult<CsMessageVO> getMessages(Long conversationId, Integer pageNum, Integer pageSize) {
        Page<CsMessage> page = new Page<>(pageNum, pageSize);
        IPage<CsMessage> messagePage = messageRepository.selectPage(page,
            new LambdaQueryWrapper<CsMessage>()
                .eq(CsMessage::getConversationId, conversationId)
                .orderByDesc(CsMessage::getCreateTime)
        );

        Map<Long, SysUser> userMap = batchGetUsers(messagePage.getRecords());
        List<CsMessageVO> records = messagePage.getRecords().stream()
            .map(msg -> convertMessageToVO(msg, userMap))
            .collect(Collectors.toList());

        return PageResult.<CsMessageVO>builder()
            .records(records)
            .total(messagePage.getTotal())
            .pages(Integer.valueOf(String.valueOf(messagePage.getPages())))
            .pageNum(pageNum)
            .pageSize(pageSize)
            .build();
    }

    @Override
    @Transactional
    public CsMessageVO sendMessage(Long userId, CsMessageSendRequest request) {
        CsConversation conversation = conversationRepository.selectById(request.getConversationId());
        if (conversation == null) {
            throw new RuntimeException("会话不存在");
        }

        // 验证权限
        if (!conversation.getUserId().equals(userId) &&
            (conversation.getAdminId() == null || !conversation.getAdminId().equals(userId))) {
            throw new RuntimeException("无权限发送消息");
        }

        CsMessage message = new CsMessage();
        message.setConversationId(request.getConversationId());
        message.setSenderId(userId);
        message.setSenderRole("USER");
        message.setMessageType(request.getMessageType());
        message.setContent(request.getContent());
        message.setAttachments(request.getAttachments());
        message.setIsRead(0);
        message.setCreateTime(LocalDateTime.now());

        // 获取用户昵称
        SysUser user = sysUserRepository.selectById(userId);
        if (user != null) {
            message.setSenderNickname(user.getNickname());
        }

        messageRepository.insert(message);

        // 更新会话的最后消息时间和未读数
        conversation.setLastMessageTime(LocalDateTime.now());
        if (conversation.getAdminId() != null) {
            Integer adminUnreadCount = conversation.getUnreadAdminCount();
            conversation.setUnreadAdminCount(adminUnreadCount == null ? 1 : adminUnreadCount + 1);
        }
        conversationRepository.updateById(conversation);

        // 推送消息给客服
        CsMessageVO messageVO = convertMessageToVO(message, Map.of(userId, user != null ? user : new SysUser()));
        try {
            webSocketService.pushNewMessage(request.getConversationId(), messageVO);
        } catch (Exception e) {
            log.warn("推送消息失败", e);
        }

        // 新增：AI 自动回复逻辑（只有会话没有人工客服时才触发）
        if (shouldTriggerAi(conversation)) {
            final Long conversationId = request.getConversationId();
            final String userMessage = request.getContent();
            final Long senderId = userId;
            // 异步调用 AI 服务，避免阻塞用户消息发送
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    if (aiChatService != null) {
                        aiChatService.processUserMessage(conversationId, userMessage, senderId);
                    }
                } catch (Exception e) {
                    log.error("AI 回复失败", e);
                }
            });
        }

        return messageVO;
    }

    /**
     * 判断是否应该触发 AI 回复
     */
    private boolean shouldTriggerAi(CsConversation conversation) {
        // AI 功能未启用
        if (!aiProperties.getEnabled()) {
            return false;
        }

        // 没有人工客服接入
        return conversation.getAdminId() == null;
    }

    @Override
    @Transactional
    public void markUserAsRead(Long userId, Long conversationId) {
        CsConversation conversation = conversationRepository.selectById(conversationId);
        if (conversation != null && conversation.getUserId().equals(userId)) {
            conversation.setUnreadUserCount(0);
            conversationRepository.updateById(conversation);

            // 标记消息为已读
            messageRepository.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CsMessage>()
                .eq(CsMessage::getConversationId, conversationId)
                .eq(CsMessage::getSenderRole, "ADMIN")
                .eq(CsMessage::getIsRead, 0)
                .set(CsMessage::getIsRead, 1)
            );
        }
    }

    @Override
    @Transactional
    public void endConversation(Long userId, Long conversationId, String reason) {
        CsConversation conversation = conversationRepository.selectById(conversationId);
        if (conversation != null && conversation.getUserId().equals(userId)) {
            conversation.setStatus(CsConversationStatus.ENDED.getCode());
            conversation.setEndReason(reason != null ? reason : "USER_END");
            conversation.setEndTime(LocalDateTime.now());
            conversationRepository.updateById(conversation);

            // 发送系统消息
            addSystemMessage(conversationId, "用户已结束会话");

            // 更新客服会话数
            if (conversation.getAdminId() != null) {
                updateAdminConversationCount(conversation.getAdminId());
            }

            // 通过WebSocket通知客服会话已结束
            try {
                webSocketService.pushConversationUpdate(convertToVO(conversation));
            } catch (Exception e) {
                log.warn("推送会话结束通知失败", e);
            }
        }
    }

    @Override
    public List<CsAdminStatusVO> getOnlineAdmins() {
        List<Long> onlineAdminIds = adminStatusRepository.findOnlineAdminIds();
        if (onlineAdminIds == null || onlineAdminIds.isEmpty()) {
            return List.of();
        }

        List<CsAdminStatus> statuses = adminStatusRepository.findByAdminIdIn(onlineAdminIds);
        return statuses.stream()
            .map(this::convertAdminStatusToVO)
            .collect(Collectors.toList());
    }

    // ==================== 管理员端 ====================

    @Override
    public PageResult<CsConversationVO> getAdminConversations(Long adminId, Integer pageNum, Integer pageSize, Integer status) {
        Page<CsConversation> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CsConversation> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(CsConversation::getStatus, status);
        }

        wrapper.orderByDesc(CsConversation::getCreateTime);

        IPage<CsConversation> conversationPage = conversationRepository.selectPage(page, wrapper);

        List<CsConversationVO> records = conversationPage.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());

        return PageResult.<CsConversationVO>builder()
            .records(records)
            .total(conversationPage.getTotal())
            .pages(Integer.valueOf(String.valueOf(conversationPage.getPages())))
            .pageNum(pageNum)
            .pageSize(pageSize)
            .build();
    }

    @Override
    @Transactional
    public CsConversationVO acceptConversation(Long adminId, Long conversationId) {
        CsConversation conversation = conversationRepository.selectById(conversationId);
        if (conversation == null) {
            throw new RuntimeException("会话不存在");
        }

        if (conversation.getStatus() != CsConversationStatus.WAITING.getCode()) {
            throw new RuntimeException("会话状态不允许接单");
        }

        // 获取管理员信息
        SysUser admin = sysUserRepository.selectById(adminId);

        conversation.setAdminId(adminId);
        if (admin != null) {
            conversation.setAdminNickname(admin.getNickname());
        }
        conversation.setStatus(CsConversationStatus.ACTIVE.getCode());
        conversation.setUnreadAdminCount(0);
        conversationRepository.updateById(conversation);

        // 发送系统消息
        addSystemMessage(conversationId, "客服已接入会话");

        // 更新客服会话数
        updateAdminConversationCount(adminId);

        return convertToVO(conversation);
    }

    @Override
    @Transactional
    public CsMessageVO adminSendMessage(Long adminId, CsMessageSendRequest request) {
        CsConversation conversation = conversationRepository.selectById(request.getConversationId());
        if (conversation == null) {
            throw new RuntimeException("会话不存在");
        }

        if (!conversation.getAdminId().equals(adminId)) {
            throw new RuntimeException("无权限发送消息");
        }

        CsMessage message = new CsMessage();
        message.setConversationId(request.getConversationId());
        message.setSenderId(adminId);
        message.setSenderRole("ADMIN");
        message.setMessageType(request.getMessageType());
        message.setContent(request.getContent());
        message.setAttachments(request.getAttachments());
        message.setIsRead(0);
        message.setCreateTime(LocalDateTime.now());

        // 获取管理员昵称
        SysUser admin = sysUserRepository.selectById(adminId);
        if (admin != null) {
            message.setSenderNickname(admin.getNickname());
        }

        messageRepository.insert(message);

        // 更新会话的最后消息时间和用户未读数
        conversation.setLastMessageTime(LocalDateTime.now());
        Integer unreadCount = conversation.getUnreadUserCount();
        conversation.setUnreadUserCount(unreadCount == null ? 1 : unreadCount + 1);
        conversationRepository.updateById(conversation);

        // 推送消息给用户
        CsMessageVO messageVO = convertMessageToVO(message, Map.of(adminId, admin != null ? admin : new SysUser()));
        try {
            webSocketService.pushNewMessage(request.getConversationId(), messageVO);
        } catch (Exception e) {
            log.warn("推送消息失败", e);
        }

        return messageVO;
    }

    @Override
    @Transactional
    public void endConversationByAdmin(Long adminId, Long conversationId, String reason) {
        CsConversation conversation = conversationRepository.selectById(conversationId);
        if (conversation != null && conversation.getAdminId() != null && conversation.getAdminId().equals(adminId)) {
            conversation.setStatus(CsConversationStatus.ENDED.getCode());
            conversation.setEndReason(reason != null ? reason : "ADMIN_END");
            conversation.setEndTime(LocalDateTime.now());
            conversationRepository.updateById(conversation);

            // 发送系统消息
            addSystemMessage(conversationId, "客服已结束会话");

            // 更新客服会话数
            updateAdminConversationCount(adminId);
        }
    }

    @Override
    @Transactional
    public void setAdminOnlineStatus(Long adminId, String nickname, boolean online) {
        CsAdminStatus status = adminStatusRepository.findByAdminId(adminId);

        if (status == null) {
            status = new CsAdminStatus();
            status.setAdminId(adminId);
            status.setAdminNickname(nickname);
            status.setIsOnline(online ? 1 : 0);
            status.setCurrentConversationCount(0);
            status.setTotalServedCount(0);
            status.setLastHeartbeat(LocalDateTime.now());
            adminStatusRepository.insert(status);
        } else {
            status.setIsOnline(online ? 1 : 0);
            status.setAdminNickname(nickname);
            status.setLastHeartbeat(LocalDateTime.now());
            adminStatusRepository.updateById(status);
        }

        // 客服上线时，自动分配等待中的会话
        if (online) {
            assignWaitingConversations(adminId, nickname);
        }
    }

    /**
     * 将等待中的会话分配给刚上线的客服
     */
    private void assignWaitingConversations(Long adminId, String nickname) {
        List<CsConversation> waitingList = conversationRepository.findWaitingConversations();
        if (waitingList == null || waitingList.isEmpty()) {
            return;
        }

        for (CsConversation conversation : waitingList) {
            conversation.setAdminId(adminId);
            conversation.setAdminNickname(nickname);
            conversation.setStatus(CsConversationStatus.ACTIVE.getCode());
            conversationRepository.updateById(conversation);

            addSystemMessage(conversation.getId(), "客服 " + nickname + " 已接入会话");

            // 通过WebSocket通知用户端
            try {
                webSocketService.pushConversationUpdate(convertToVO(conversation));
            } catch (Exception e) {
                log.warn("推送会话更新失败", e);
            }

            // 推送专门的分配通知给客服
            try {
                webSocketService.pushConversationAssigned(adminId, convertToVO(conversation), true);
            } catch (Exception e) {
                log.warn("推送会话分配通知失败", e);
            }
        }

        // 更新客服当前会话数
        updateAdminConversationCount(adminId);
    }

    @Override
    @Transactional
    public void updateAdminHeartbeat(Long adminId) {
        CsAdminStatus status = adminStatusRepository.findByAdminId(adminId);
        if (status != null) {
            status.setLastHeartbeat(LocalDateTime.now());
            adminStatusRepository.updateById(status);
        }
    }

    @Override
    @Transactional
    public void markAdminAsRead(Long adminId, Long conversationId) {
        CsConversation conversation = conversationRepository.selectById(conversationId);
        if (conversation != null && conversation.getAdminId() != null && conversation.getAdminId().equals(adminId)) {
            conversation.setUnreadAdminCount(0);
            conversationRepository.updateById(conversation);

            // 标记消息为已读
            messageRepository.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CsMessage>()
                .eq(CsMessage::getConversationId, conversationId)
                .eq(CsMessage::getSenderRole, "USER")
                .eq(CsMessage::getIsRead, 0)
                .set(CsMessage::getIsRead, 1)
            );
        }
    }

    @Override
    public CsConversationDetailVO getConversationDetail(Long conversationId) {
        CsConversation conversation = conversationRepository.selectById(conversationId);
        if (conversation == null) {
            return null;
        }

        List<CsMessage> messages = messageRepository.selectList(
            new LambdaQueryWrapper<CsMessage>()
                .eq(CsMessage::getConversationId, conversationId)
                .orderByDesc(CsMessage::getCreateTime)
                .last("LIMIT 200")
        );
        Collections.reverse(messages);

        Map<Long, SysUser> userMap = batchGetUsers(messages);
        return CsConversationDetailVO.builder()
            .conversation(convertToVO(conversation))
            .messages(messages.stream().map(msg -> convertMessageToVO(msg, userMap)).collect(Collectors.toList()))
            .build();
    }

    // ==================== 私有方法 ====================

    /**
     * 分配客服（负载最低策略）
     */
    private void assignAdminToConversation(CsConversation conversation) {
        List<Long> onlineAdminIds = adminStatusRepository.findOnlineAdminIds();

        if (onlineAdminIds == null || onlineAdminIds.isEmpty()) {
            // 没有在线客服，发送系统消息
            addSystemMessage(conversation.getId(), "暂无客服在线，请稍后再试");
            return;
        }

        // 查询各客服当前会话数，选择最闲的
        List<CsAdminStatus> statuses = adminStatusRepository.findByAdminIdIn(onlineAdminIds);
        CsAdminStatus selected = statuses.stream()
            .min((a, b) -> Integer.compare(
                a.getCurrentConversationCount() != null ? a.getCurrentConversationCount() : 0,
                b.getCurrentConversationCount() != null ? b.getCurrentConversationCount() : 0
            ))
            .orElse(null);

        if (selected != null) {
            conversation.setAdminId(selected.getAdminId());
            conversation.setAdminNickname(selected.getAdminNickname());
            conversation.setStatus(CsConversationStatus.ACTIVE.getCode());
            conversationRepository.updateById(conversation);

            // 更新客服会话数
            updateAdminConversationCount(selected.getAdminId());

            // 发送系统消息
            addSystemMessage(conversation.getId(), "客服 " + selected.getAdminNickname() + " 已接入会话");

            // 通过WebSocket通知客服有新会话
            try {
                webSocketService.pushConversationUpdate(convertToVO(conversation));
            } catch (Exception e) {
                log.warn("推送新会话通知失败", e);
            }
        }
    }

    /**
     * 更新客服当前会话数
     */
    private void updateAdminConversationCount(Long adminId) {
        if (adminId == null) return;

        int count = conversationRepository.countActiveByAdminId(adminId);
        CsAdminStatus status = adminStatusRepository.findByAdminId(adminId);
        if (status != null) {
            status.setCurrentConversationCount(count);
            if (count == 0) {
                status.setTotalServedCount(status.getTotalServedCount() + 1);
            }
            adminStatusRepository.updateById(status);
        }
    }

    /**
     * 添加系统消息
     */
    private void addSystemMessage(Long conversationId, String content) {
        CsMessage message = new CsMessage();
        message.setConversationId(conversationId);
        message.setSenderId(0L);
        message.setSenderRole("SYSTEM");
        message.setSenderNickname("系统");
        message.setMessageType(CsMessageType.SYSTEM.getCode());
        message.setContent(content);
        message.setIsRead(0);
        message.setCreateTime(LocalDateTime.now());
        messageRepository.insert(message);
    }

    /**
     * 转换会话实体为VO
     */
    private CsConversationVO convertToVO(CsConversation conversation) {
        if (conversation == null) return null;

        return CsConversationVO.builder()
            .id(conversation.getId())
            .sessionNo(conversation.getSessionNo())
            .userId(conversation.getUserId())
            .userNickname(conversation.getUserNickname())
            .userAvatar(conversation.getUserAvatar())
            .adminId(conversation.getAdminId())
            .adminNickname(conversation.getAdminNickname())
            .status(conversation.getStatus())
            .statusDesc(CsConversationStatus.fromCode(conversation.getStatus()) != null ?
                CsConversationStatus.fromCode(conversation.getStatus()).getDescription() : "未知")
            .endReason(conversation.getEndReason())
            .endTime(conversation.getEndTime())
            .unreadUserCount(conversation.getUnreadUserCount())
            .unreadAdminCount(conversation.getUnreadAdminCount())
            .createTime(conversation.getCreateTime())
            .lastMessageTime(conversation.getLastMessageTime())
            .build();
    }

    /**
     * 批量获取消息发送者用户信息（消除 N+1 查询）
     */
    private Map<Long, SysUser> batchGetUsers(List<CsMessage> messages) {
        Set<Long> senderIds = messages.stream()
            .map(CsMessage::getSenderId)
            .filter(id -> id != null && id > 0)
            .collect(Collectors.toSet());
        if (senderIds.isEmpty()) {
            return Map.of();
        }
        return sysUserRepository.selectBatchIds(senderIds).stream()
            .collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));
    }

    /**
     * 转换消息实体为VO（使用预加载的用户Map）
     */
    private CsMessageVO convertMessageToVO(CsMessage message, Map<Long, SysUser> userMap) {
        if (message == null) return null;

        String senderAvatar = null;
        SysUser user = userMap.get(message.getSenderId());
        if (user != null) {
            senderAvatar = user.getAvatar();
        }

        return CsMessageVO.builder()
            .id(message.getId())
            .conversationId(message.getConversationId())
            .senderId(message.getSenderId())
            .senderRole(message.getSenderRole())
            .senderNickname(message.getSenderNickname())
            .senderAvatar(senderAvatar)
            .messageType(message.getMessageType())
            .content(message.getContent())
            .attachments(message.getAttachments())
            .isRead(message.getIsRead() != null && message.getIsRead() == 1)
            .createTime(message.getCreateTime())
            .build();
    }

    /**
     * 转换客服状态实体为VO
     */
    private CsAdminStatusVO convertAdminStatusToVO(CsAdminStatus status) {
        if (status == null) return null;

        return CsAdminStatusVO.builder()
            .adminId(status.getAdminId())
            .adminNickname(status.getAdminNickname())
            .isOnline(status.getIsOnline() != null && status.getIsOnline() == 1)
            .currentConversationCount(status.getCurrentConversationCount())
            .totalServedCount(status.getTotalServedCount())
            .build();
    }

    // 超时时间：5分钟
    private static final long CONVERSATION_TIMEOUT_MINUTES = 5;

    @Override
    public void closeExpiredConversations() {
        // SQL 层直接过滤超时会话，避免全表扫描到内存
        List<CsConversation> expiredConversations = conversationRepository.findExpiredConversations(CONVERSATION_TIMEOUT_MINUTES);
        if (expiredConversations == null || expiredConversations.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Set<Long> adminIds = new HashSet<>();
        for (CsConversation conversation : expiredConversations) {
            conversation.setStatus(CsConversationStatus.ENDED.getCode());
            conversation.setEndReason("TIMEOUT");
            conversation.setEndTime(now);
            conversationRepository.updateById(conversation);

            addSystemMessage(conversation.getId(), "会话因长时间无活动已自动结束");

            if (conversation.getAdminId() != null) {
                adminIds.add(conversation.getAdminId());
            }

            log.info("自动关闭超时会话: {}, 用户: {}, 客服: {}",
                conversation.getId(), conversation.getUserId(), conversation.getAdminId());
        }

        // 批量更新客服会话数（去重后）
        for (Long adminId : adminIds) {
            updateAdminConversationCount(adminId);
        }
    }

    @Override
    public CsStatsVO getStats() {
        CsStatsVO stats = new CsStatsVO();
        // 当前进行中的会话数
        stats.setActiveConversations(conversationRepository.countByStatus(CsConversationStatus.ACTIVE.getCode()));
        // 累计服务数（已结束的会话）
        stats.setTotalServed(conversationRepository.countByStatus(CsConversationStatus.ENDED.getCode()));
        return stats;
    }
}
