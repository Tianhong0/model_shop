package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.ai.knowledge.BusinessKnowledgeBase;
import org.majun.backend.ai.model.*;
import org.majun.backend.ai.prompt.PromptTemplateManager;
import org.majun.backend.ai.provider.AiModelProvider;
import org.majun.backend.config.AiProperties;
import org.majun.backend.entity.CsAdminStatus;
import org.majun.backend.entity.CsConversation;
import org.majun.backend.entity.CsMessage;
import org.majun.backend.enums.CsConversationStatus;
import org.majun.backend.enums.CsMessageType;
import org.majun.backend.repository.CsAdminStatusRepository;
import org.majun.backend.repository.CsConversationRepository;
import org.majun.backend.repository.CsMessageRepository;
import org.majun.backend.service.AiChatService;
import org.majun.backend.service.CustomerServiceWebSocketService;
import org.majun.backend.vo.AiChatVO;
import org.majun.backend.vo.CsConversationVO;
import org.majun.backend.vo.CsMessageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AI 聊天服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final AiModelProvider aiModelProvider;
    private final BusinessKnowledgeBase knowledgeBase;
    private final PromptTemplateManager promptManager;
    private final AiProperties aiProperties;
    private final CsConversationRepository conversationRepository;
    private final CsMessageRepository messageRepository;
    private final CsAdminStatusRepository adminStatusRepository;
    private final CustomerServiceWebSocketService webSocketService;
    private final ObjectMapper objectMapper;

    /**
     * 记录无法回答的次数
     */
    private final Map<Long, Integer> noAnswerCountMap = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public AiChatVO processUserMessage(Long conversationId, String userMessage, Long userId) {
        log.info("处理AI消息: conversationId={}, message={}", conversationId, userMessage);

        // 检查是否需要转人工
        if (shouldTransferToHuman(conversationId, userMessage)) {
            log.info("触发转人工: conversationId={}", conversationId);
            return transferToHuman(conversationId, userId);
        }

        // 意图识别
        IntentResult intent = classifyIntent(userMessage);
        log.info("意图识别结果: {}, 置信度: {}", intent.getIntent(), intent.getConfidence());

        // 获取业务上下文
        String businessContext = knowledgeBase.getContext(intent.getIntent());

        // 构建系统提示词
        String systemPrompt = promptManager.buildSystemPrompt(intent.getIntent(), businessContext);

        // 获取历史对话
        List<ChatMessage> history = getConversationHistory(conversationId, 10);

        // 调用 AI 生成回复
        AiChatRequest request = AiChatRequest.builder()
                .conversationId(conversationId)
                .systemPrompt(systemPrompt)
                .userMessage(userMessage)
                .history(history)
                .build();

        AiChatResponse aiResponse = aiModelProvider.chat(request);

        // 检查 AI 是否成功回答
        if (!aiResponse.getSuccess() || isAiUnableToAnswer(aiResponse)) {
            incrementNoAnswerCount(conversationId);
            if (getNoAnswerCount(conversationId) >= aiProperties.getCustomerService()
                    .getTransfer().getNoAnswerTurns()) {
                log.info("AI连续无法回答，转人工: conversationId={}", conversationId);
                return transferToHuman(conversationId, userId);
            }
        } else {
            // 成功回答，重置计数
            resetNoAnswerCount(conversationId);
        }

        // 保存 AI 消息
        CsMessage aiMessage = saveAiMessage(conversationId, aiResponse.getContent());

        // 推送消息给用户
        pushAiMessage(conversationId, aiMessage);

        return AiChatVO.builder()
                .messageId(aiMessage.getId())
                .content(aiResponse.getContent())
                .intent(intent.getIntent().name())
                .confidence(intent.getConfidence())
                .provider(aiResponse.getProvider())
                .tokensUsed(aiResponse.getTokensUsed())
                .success(aiResponse.getSuccess())
                .build();
    }

    @Override
    public String generateSummary(Long conversationId) {
        List<CsMessage> messages = messageRepository.selectList(
                new LambdaQueryWrapper<CsMessage>()
                        .eq(CsMessage::getConversationId, conversationId)
                        .orderByAsc(CsMessage::getCreateTime)
        );

        if (messages.isEmpty()) {
            return "暂无对话内容";
        }

        String conversationText = messages.stream()
                .map(m -> m.getSenderRole() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

        String summaryPrompt = promptManager.getSummaryPrompt(conversationText);

        AiChatRequest request = AiChatRequest.builder()
                .systemPrompt(promptManager.getSummarySystemPrompt())
                .userMessage(summaryPrompt)
                .build();

        AiChatResponse response = aiModelProvider.chat(request);

        return response.getSuccess() ? response.getContent() : "无法生成摘要";
    }

    @Override
    public IntentResult classifyIntent(String userMessage) {
        String prompt = promptManager.getIntentClassificationPrompt(userMessage);

        AiChatRequest request = AiChatRequest.builder()
                .systemPrompt(promptManager.getIntentSystemPrompt())
                .userMessage(prompt)
                .build();

        AiChatResponse response = aiModelProvider.chat(request);

        if (!response.getSuccess()) {
            return IntentResult.builder()
                    .intent(IntentType.OTHER)
                    .confidence(0.5)
                    .reason("AI服务暂时不可用")
                    .build();
        }

        try {
            // 解析 JSON 响应
            String content = response.getContent().trim();
            // 移除可能的 markdown 代码块标记
            if (content.startsWith("```json")) {
                content = content.substring(7);
            }
            if (content.startsWith("```")) {
                content = content.substring(3);
            }
            if (content.endsWith("```")) {
                content = content.substring(0, content.length() - 3);
            }
            content = content.trim();

            JsonNode json = objectMapper.readTree(content);
            String intentStr = json.path("intent").asText("OTHER");
            IntentType intentType;
            try {
                intentType = IntentType.valueOf(intentStr);
            } catch (IllegalArgumentException e) {
                intentType = IntentType.OTHER;
            }

            return IntentResult.builder()
                    .intent(intentType)
                    .confidence(json.path("confidence").asDouble(0.5))
                    .reason(json.path("reason").asText())
                    .build();
        } catch (Exception e) {
            log.warn("意图解析失败", e);
            return IntentResult.builder()
                    .intent(IntentType.OTHER)
                    .confidence(0.5)
                    .reason("解析失败")
                    .build();
        }
    }

    @Override
    public boolean shouldTransferToHuman(Long conversationId, String userMessage) {
        if (!aiProperties.getCustomerService().getTransfer().getEnabled()) {
            log.info("转人工功能未启用");
            return false;
        }

        List<String> keywords = aiProperties.getCustomerService().getTransfer().getKeywords();
        if (keywords == null) {
            log.info("未配置转人工关键词");
            return false;
        }

        boolean shouldTransfer = keywords.stream()
                .anyMatch(keyword -> userMessage.contains(keyword));

        log.info("检查转人工关键词: userMessage={}, keywords={}, result={}", userMessage, keywords, shouldTransfer);

        return shouldTransfer;
    }

    @Override
    @Transactional
    public AiChatVO transferToHuman(Long conversationId, Long userId) {
        log.info("转人工客服: conversationId={}", conversationId);

        CsConversation conversation = conversationRepository.selectById(conversationId);
        if (conversation == null) {
            return AiChatVO.builder()
                    .success(false)
                    .errorMessage("会话不存在")
                    .build();
        }

        // 尝试自动分配在线客服
        List<Long> onlineAdminIds = adminStatusRepository.findOnlineAdminIds();
        log.info("当前在线客服: {}", onlineAdminIds);

        if (onlineAdminIds != null && !onlineAdminIds.isEmpty()) {
            // 有在线客服，自动分配（负载最低策略）
            List<CsAdminStatus> statuses = adminStatusRepository.findByAdminIdIn(onlineAdminIds);
            CsAdminStatus selected = statuses.stream()
                    .min((a, b) -> Integer.compare(
                            a.getCurrentConversationCount() != null ? a.getCurrentConversationCount() : 0,
                            b.getCurrentConversationCount() != null ? b.getCurrentConversationCount() : 0
                    ))
                    .orElse(null);

            if (selected != null) {
                log.info("自动分配客服: adminId={}, nickname={}", selected.getAdminId(), selected.getAdminNickname());

                // 更新会话
                conversation.setAdminId(selected.getAdminId());
                conversation.setAdminNickname(selected.getAdminNickname());
                conversation.setStatus(CsConversationStatus.ACTIVE.getCode());
                conversationRepository.updateById(conversation);

                // 更新客服会话数
                updateAdminConversationCount(selected.getAdminId());

                // 添加系统消息
                CsMessage systemMessage = new CsMessage();
                systemMessage.setConversationId(conversationId);
                systemMessage.setSenderId(0L);
                systemMessage.setSenderRole("SYSTEM");
                systemMessage.setSenderNickname("系统");
                systemMessage.setMessageType(CsMessageType.SYSTEM.getCode());
                systemMessage.setContent("已为您接入客服 " + selected.getAdminNickname() + "，请稍候...");
                systemMessage.setCreateTime(LocalDateTime.now());
                messageRepository.insert(systemMessage);

                // 推送消息给用户
                pushAiMessage(conversationId, systemMessage);

                // 推送会话更新给客服
                try {
                    CsConversationVO conversationVO = CsConversationVO.builder()
                            .id(conversation.getId())
                            .sessionNo(conversation.getSessionNo())
                            .userId(conversation.getUserId())
                            .userNickname(conversation.getUserNickname())
                            .userAvatar(conversation.getUserAvatar())
                            .adminId(conversation.getAdminId())
                            .adminNickname(conversation.getAdminNickname())
                            .status(conversation.getStatus())
                            .createTime(conversation.getCreateTime())
                            .lastMessageTime(conversation.getLastMessageTime())
                            .build();
                    webSocketService.pushConversationUpdate(conversationVO);
                } catch (Exception e) {
                    log.error("推送会话更新失败", e);
                }

                return AiChatVO.builder()
                        .messageId(systemMessage.getId())
                        .content(systemMessage.getContent())
                        .transferToHuman(true)
                        .success(true)
                        .build();
            }
        }

        // 没有在线客服，保持等待状态
        log.info("没有在线客服，会话进入等待状态: conversationId={}", conversationId);

        // 生成会话摘要
        String summary = generateSummary(conversationId);

        // 更新会话状态为等待
        conversation.setStatus(CsConversationStatus.WAITING.getCode());
        conversation.setAdminId(null);
        conversationRepository.updateById(conversation);

        // 添加系统消息
        CsMessage systemMessage = new CsMessage();
        systemMessage.setConversationId(conversationId);
        systemMessage.setSenderId(0L);
        systemMessage.setSenderRole("SYSTEM");
        systemMessage.setSenderNickname("系统");
        systemMessage.setMessageType(CsMessageType.SYSTEM.getCode());
        systemMessage.setContent("正在为您转接人工客服，请稍候...\n\n📋 问题摘要：" + summary);
        systemMessage.setCreateTime(LocalDateTime.now());
        messageRepository.insert(systemMessage);

        // 重置无法回答计数
        resetNoAnswerCount(conversationId);

        // 推送消息给用户
        pushAiMessage(conversationId, systemMessage);

        // 推送会话更新给所有在线客服（通知有新会话等待接入）
        try {
            CsConversationVO conversationVO = CsConversationVO.builder()
                    .id(conversation.getId())
                    .sessionNo(conversation.getSessionNo())
                    .userId(conversation.getUserId())
                    .userNickname(conversation.getUserNickname())
                    .userAvatar(conversation.getUserAvatar())
                    .status(conversation.getStatus())
                    .createTime(conversation.getCreateTime())
                    .lastMessageTime(conversation.getLastMessageTime())
                    .build();
            webSocketService.pushConversationUpdate(conversationVO);
            log.info("已推送转人工会话通知: conversationId={}", conversationId);
        } catch (Exception e) {
            log.error("推送转人工通知失败", e);
        }

        return AiChatVO.builder()
                .messageId(systemMessage.getId())
                .content(systemMessage.getContent())
                .transferToHuman(true)
                .summary(summary)
                .success(true)
                .build();
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
            adminStatusRepository.updateById(status);
        }
    }

    /**
     * 保存 AI 消息
     */
    private CsMessage saveAiMessage(Long conversationId, String content) {
        CsMessage message = new CsMessage();
        message.setConversationId(conversationId);
        message.setSenderId(-1L); // AI 的 senderId 设为 -1
        message.setSenderRole("AI");
        message.setSenderNickname("智能助手");
        message.setMessageType(CsMessageType.TEXT.getCode());
        message.setContent(content);
        message.setIsRead(0);
        message.setCreateTime(LocalDateTime.now());
        messageRepository.insert(message);
        return message;
    }

    /**
     * 推送 AI 消息给用户
     */
    private void pushAiMessage(Long conversationId, CsMessage message) {
        CsMessageVO messageVO = CsMessageVO.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .senderRole(message.getSenderRole())
                .senderNickname(message.getSenderNickname())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .isRead(false)
                .createTime(message.getCreateTime())
                .build();

        webSocketService.pushNewMessage(conversationId, messageVO);
    }

    /**
     * 获取对话历史
     */
    private List<ChatMessage> getConversationHistory(Long conversationId, int limit) {
        List<CsMessage> messages = messageRepository.selectList(
                new LambdaQueryWrapper<CsMessage>()
                        .eq(CsMessage::getConversationId, conversationId)
                        .in(CsMessage::getSenderRole, "USER", "AI")
                        .orderByDesc(CsMessage::getCreateTime)
                        .last("LIMIT " + limit)
        );

        List<ChatMessage> history = new ArrayList<>();
        // 反转顺序，使历史消息按时间正序
        for (int i = messages.size() - 1; i >= 0; i--) {
            CsMessage msg = messages.get(i);
            String role = "USER".equals(msg.getSenderRole()) ? "user" : "assistant";
            history.add(ChatMessage.builder()
                    .role(role)
                    .content(msg.getContent())
                    .build());
        }
        return history;
    }

    /**
     * 判断 AI 是否无法回答
     */
    private boolean isAiUnableToAnswer(AiChatResponse response) {
        if (!response.getSuccess()) {
            return true;
        }
        String content = response.getContent().toLowerCase();
        // 检查是否包含无法回答的关键词
        return content.contains("无法回答") ||
               content.contains("不清楚") ||
               content.contains("不知道") ||
               content.contains("转人工") ||
               content.contains("人工客服");
    }

    private void incrementNoAnswerCount(Long conversationId) {
        noAnswerCountMap.merge(conversationId, 1, Integer::sum);
    }

    private int getNoAnswerCount(Long conversationId) {
        return noAnswerCountMap.getOrDefault(conversationId, 0);
    }

    private void resetNoAnswerCount(Long conversationId) {
        noAnswerCountMap.remove(conversationId);
    }
}
