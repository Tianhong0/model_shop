package org.majun.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /**
     * 是否启用 AI 功能
     */
    private Boolean enabled = true;

    /**
     * 智谱 AI 配置
     */
    private ZhipuConfig zhipu = new ZhipuConfig();

    /**
     * 智能客服配置
     */
    private CustomerServiceConfig customerService = new CustomerServiceConfig();

    @Data
    public static class ZhipuConfig {
        /**
         * API Key
         */
        private String apiKey;

        /**
         * 模型名称
         */
        private String model = "glm-4-flash";

        /**
         * API 地址
         */
        private String baseUrl = "https://open.bigmodel.cn/api/paas/v4";

        /**
         * 最大 Token 数
         */
        private Integer maxTokens = 1024;

        /**
         * 温度参数
         */
        private Double temperature = 0.7;

        /**
         * Embedding 模型名称
         */
        private String embeddingModel = "embedding-3";

        /**
         * Embedding 向量维度
         */
        private Integer embeddingDimension = 1024;
    }

    @Data
    public static class CustomerServiceConfig {
        /**
         * 自动回复配置
         */
        private AutoReplyConfig autoReply = new AutoReplyConfig();

        /**
         * 转人工配置
         */
        private TransferConfig transfer = new TransferConfig();
    }

    @Data
    public static class AutoReplyConfig {
        /**
         * 是否启用自动回复
         */
        private Boolean enabled = true;

        /**
         * AI 最大对话轮次
         */
        private Integer maxTurns = 3;
    }

    @Data
    public static class TransferConfig {
        /**
         * 是否启用智能转人工
         */
        private Boolean enabled = true;

        /**
         * 触发转人工的关键词
         */
        private List<String> keywords = List.of("人工客服", "转人工", "投诉", "退款");

        /**
         * AI 连续无法回答的轮次，超过后自动转人工
         */
        private Integer noAnswerTurns = 2;
    }
}
