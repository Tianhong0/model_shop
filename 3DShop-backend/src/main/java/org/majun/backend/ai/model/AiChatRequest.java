package org.majun.backend.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 聊天请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {

    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 用户消息
     */
    private String userMessage;

    /**
     * 历史对话
     */
    private List<ChatMessage> history;

    /**
     * 最大 Token 数
     */
    private Integer maxTokens;

    /**
     * 温度参数
     */
    private Double temperature;
}
