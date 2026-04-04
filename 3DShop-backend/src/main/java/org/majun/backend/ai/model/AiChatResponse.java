package org.majun.backend.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 聊天响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {

    /**
     * AI 回复内容
     */
    private String content;

    /**
     * AI 提供商
     */
    private String provider;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * 消耗的 Token 数
     */
    private Integer tokensUsed;

    /**
     * 响应时间(毫秒)
     */
    private Long responseTimeMs;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;
}
