package org.majun.backend.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.majun.backend.ai.model.AiChatRequest;
import org.majun.backend.ai.model.AiChatResponse;
import org.majun.backend.ai.model.ChatMessage;
import org.majun.backend.config.AiProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智谱 GLM AI 模型提供者实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZhipuAiProvider implements AiModelProvider {

    private final AiProperties aiProperties;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        long startTime = System.currentTimeMillis();

        AiProperties.ZhipuConfig config = aiProperties.getZhipu();

        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getModel());
            requestBody.put("messages", buildMessages(request));
            requestBody.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : config.getMaxTokens());
            requestBody.put("temperature", request.getTemperature() != null ? request.getTemperature() : config.getTemperature());

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            // 构建 HTTP 请求
            String url = config.getBaseUrl() + "/chat/completions";
            Request httpRequest = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, JSON_MEDIA_TYPE))
                    .build();

            // 发送请求
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                long responseTime = System.currentTimeMillis() - startTime;

                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("智谱 AI 调用失败: status={}, body={}", response.code(), errorBody);
                    return AiChatResponse.builder()
                            .success(false)
                            .provider(getProviderName())
                            .responseTimeMs(responseTime)
                            .errorMessage("AI 服务调用失败: " + response.code())
                            .build();
                }

                String responseBody = response.body().string();
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                // 解析响应
                String content = jsonNode.path("choices")
                        .path(0)
                        .path("message")
                        .path("content")
                        .asText();

                int totalTokens = jsonNode.path("usage")
                        .path("total_tokens")
                        .asInt(0);

                log.info("智谱 AI 调用成功: tokens={}, responseTime={}ms", totalTokens, responseTime);

                return AiChatResponse.builder()
                        .content(content)
                        .provider(getProviderName())
                        .model(config.getModel())
                        .tokensUsed(totalTokens)
                        .responseTimeMs(responseTime)
                        .success(true)
                        .build();

            }
        } catch (IOException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("智谱 AI 调用异常", e);
            return AiChatResponse.builder()
                    .success(false)
                    .provider(getProviderName())
                    .responseTimeMs(responseTime)
                    .errorMessage("AI 服务暂时不可用: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 构建消息列表
     */
    private List<Map<String, String>> buildMessages(AiChatRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();

        // 添加系统提示词
        if (StringUtils.hasText(request.getSystemPrompt())) {
            messages.add(Map.of(
                    "role", "system",
                    "content", request.getSystemPrompt()
            ));
        }

        // 添加历史对话
        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            for (ChatMessage msg : request.getHistory()) {
                messages.add(Map.of(
                        "role", msg.getRole(),
                        "content", msg.getContent()
                ));
            }
        }

        // 添加当前用户消息
        messages.add(Map.of(
                "role", "user",
                "content", request.getUserMessage()
        ));

        return messages;
    }

    @Override
    public String getProviderName() {
        return "zhipu";
    }

    @Override
    public boolean isAvailable() {
        return StringUtils.hasText(aiProperties.getZhipu().getApiKey());
    }
}
