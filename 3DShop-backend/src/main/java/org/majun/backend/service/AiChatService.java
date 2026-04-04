package org.majun.backend.service;

import org.majun.backend.ai.model.IntentResult;
import org.majun.backend.vo.AiChatVO;

/**
 * AI 聊天服务接口
 */
public interface AiChatService {

    /**
     * 处理用户消息，生成 AI 回复
     *
     * @param conversationId 会话ID
     * @param userMessage    用户消息
     * @param userId         用户ID
     * @return AI 回复结果
     */
    AiChatVO processUserMessage(Long conversationId, String userMessage, Long userId);

    /**
     * 生成会话摘要
     *
     * @param conversationId 会话ID
     * @return 摘要内容
     */
    String generateSummary(Long conversationId);

    /**
     * 识别用户意图
     *
     * @param userMessage 用户消息
     * @return 意图识别结果
     */
    IntentResult classifyIntent(String userMessage);

    /**
     * 检查是否应该转人工
     *
     * @param conversationId 会话ID
     * @param userMessage    用户消息
     * @return 是否应该转人工
     */
    boolean shouldTransferToHuman(Long conversationId, String userMessage);

    /**
     * 转人工客服
     *
     * @param conversationId 会话ID
     * @param userId         用户ID
     * @return 转人工结果
     */
    AiChatVO transferToHuman(Long conversationId, Long userId);
}
