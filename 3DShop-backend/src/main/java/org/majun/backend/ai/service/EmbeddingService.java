package org.majun.backend.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.majun.backend.config.AiProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智谱 AI Embedding 服务
 * 用于生成文本的向量表示，支持语义搜索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final AiProperties aiProperties;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    /**
     * 生成单个文本的向量嵌入
     *
     * @param text 输入文本
     * @return 向量数组，失败返回 null
     */
    public float[] generateEmbedding(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }

        List<float[]> results = generateEmbeddings(List.of(text));
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 批量生成文本的向量嵌入
     *
     * @param texts 输入文本列表
     * @return 向量数组列表
     */
    public List<float[]> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }

        AiProperties.ZhipuConfig config = aiProperties.getZhipu();
        if (!StringUtils.hasText(config.getApiKey())) {
            log.warn("智谱 AI API Key 未配置，跳过 Embedding 生成");
            return new ArrayList<>();
        }

        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getEmbeddingModel());
            requestBody.put("input", texts);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            // 构建 HTTP 请求
            String url = config.getBaseUrl() + "/embeddings";
            Request httpRequest = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, JSON_MEDIA_TYPE))
                    .build();

            // 发送请求
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("智谱 Embedding API 调用失败: status={}, body={}", response.code(), errorBody);
                    return new ArrayList<>();
                }

                String responseBody = response.body().string();
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                // 解析向量数据
                List<float[]> embeddings = new ArrayList<>();
                JsonNode dataNode = jsonNode.path("data");

                if (dataNode.isArray()) {
                    for (JsonNode item : dataNode) {
                        JsonNode embeddingNode = item.path("embedding");
                        if (embeddingNode.isArray()) {
                            float[] embedding = new float[embeddingNode.size()];
                            for (int i = 0; i < embeddingNode.size(); i++) {
                                embedding[i] = (float) embeddingNode.get(i).asDouble();
                            }
                            embeddings.add(embedding);
                        }
                    }
                }

                log.info("智谱 Embedding 生成成功: {} 个向量", embeddings.size());
                return embeddings;
            }

        } catch (IOException e) {
            log.error("智谱 Embedding API 调用异常", e);
            return new ArrayList<>();
        }
    }

    /**
     * 计算两个向量的余弦相似度
     *
     * @param vec1 向量1
     * @param vec2 向量2
     * @return 相似度值，范围 [-1, 1]，1 表示完全相似
     */
    public float cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null || vec1.length != vec2.length) {
            return 0f;
        }

        float dotProduct = 0f;
        float norm1 = 0f;
        float norm2 = 0f;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0f;
        }

        return dotProduct / (float) (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 检查服务是否可用
     */
    public boolean isAvailable() {
        return StringUtils.hasText(aiProperties.getZhipu().getApiKey());
    }

    /**
     * 获取向量维度
     */
    public int getDimension() {
        return aiProperties.getZhipu().getEmbeddingDimension();
    }
}
