package org.majun.backend.ai.provider;

import org.majun.backend.ai.model.AiChatRequest;
import org.majun.backend.ai.model.AiChatResponse;

/**
 * AI 模型提供者接口
 */
public interface AiModelProvider {

    /**
     * 生成 AI 回复
     *
     * @param request 请求参数
     * @return AI 响应
     */
    AiChatResponse chat(AiChatRequest request);

    /**
     * 获取提供者名称
     *
     * @return 提供者名称
     */
    String getProviderName();

    /**
     * 检查服务是否可用
     *
     * @return 是否可用
     */
    boolean isAvailable();
}
