package org.majun.backend.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.config.AiProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 向量存储服务
 * 使用 Redis 存储商品/模型的向量嵌入，支持语义搜索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 向量索引键前缀
     */
    private static final String VECTOR_INDEX_KEY = "vector:index:product";

    /**
     * 向量数据键前缀 (存储向量数据)
     */
    private static final String VECTOR_DATA_KEY_PREFIX = "vector:data:";

    /**
     * 向量元数据键前缀 (存储商品信息)
     */
    private static final String VECTOR_META_KEY_PREFIX = "vector:meta:";

    /**
     * 索引版本键
     */
    private static final String VECTOR_VERSION_KEY = "vector:version";

    /**
     * 向量缓存过期时间（7天）
     */
    private static final long VECTOR_EXPIRE_SECONDS = 7 * 24 * 60 * 60;

    /**
     * 存储商品向量
     *
     * @param productId    商品ID（可以是模型ID等）
     * @param productType  商品类型 (MODEL, BOUNTY 等)
     * @param embedding    向量数据
     * @param metadata     元数据（名称、描述等）
     */
    public void storeVector(Long productId, String productType, float[] embedding, Map<String, String> metadata) {
        if (productId == null || embedding == null || embedding.length == 0) {
            return;
        }

        String key = buildVectorKey(productType, productId);

        try {
            // 存储向量数据
            String embeddingJson = vectorToJson(embedding);
            redisTemplate.opsForValue().set(key, embeddingJson, VECTOR_EXPIRE_SECONDS, TimeUnit.SECONDS);

            // 存储元数据
            String metaKey = VECTOR_META_KEY_PREFIX + productType + ":" + productId;
            if (metadata != null && !metadata.isEmpty()) {
                String metaJson = objectMapper.writeValueAsString(metadata);
                redisTemplate.opsForValue().set(metaKey, metaJson, VECTOR_EXPIRE_SECONDS, TimeUnit.SECONDS);
            }

            // 添加到索引集合
            redisTemplate.opsForSet().add(VECTOR_INDEX_KEY, productType + ":" + productId);

            log.debug("向量存储成功: productId={}, type={}", productId, productType);
        } catch (JsonProcessingException e) {
            log.error("向量存储失败: productId={}", productId, e);
        }
    }

    /**
     * 批量存储商品向量
     */
    public void storeVectors(List<VectorData> vectors) {
        if (vectors == null || vectors.isEmpty()) {
            return;
        }

        for (VectorData data : vectors) {
            storeVector(data.getProductId(), data.getProductType(), data.getEmbedding(), data.getMetadata());
        }

        log.info("批量存储向量完成: {} 条", vectors.size());
    }

    /**
     * 获取商品向量
     */
    public float[] getVector(Long productId, String productType) {
        String key = buildVectorKey(productType, productId);
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            return null;
        }

        return jsonToVector(json);
    }

    /**
     * 获取商品元数据
     */
    public Map<String, String> getMetadata(Long productId, String productType) {
        String metaKey = VECTOR_META_KEY_PREFIX + productType + ":" + productId;
        String json = redisTemplate.opsForValue().get(metaKey);

        if (json == null) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.error("解析元数据失败: productId={}", productId, e);
            return new HashMap<>();
        }
    }

    /**
     * 删除商品向量
     */
    public void deleteVector(Long productId, String productType) {
        String key = buildVectorKey(productType, productId);
        String metaKey = VECTOR_META_KEY_PREFIX + productType + ":" + productId;

        redisTemplate.delete(key);
        redisTemplate.delete(metaKey);
        redisTemplate.opsForSet().remove(VECTOR_INDEX_KEY, productType + ":" + productId);

        log.debug("向量删除成功: productId={}, type={}", productId, productType);
    }

    /**
     * 语义搜索 - 找出最相似的商品
     *
     * @param queryEmbedding 查询向量
     * @param productType    商品类型（可选，null 表示搜索所有类型）
     * @param topK           返回数量
     * @param threshold      相似度阈值 (0-1)
     * @return 相似商品列表，按相似度降序排列
     */
    public List<SearchResult> search(float[] queryEmbedding, String productType, int topK, float threshold) {
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            return new ArrayList<>();
        }

        List<SearchResult> results = new ArrayList<>();

        // 获取所有索引的商品ID
        Set<String> allIds = redisTemplate.opsForSet().members(VECTOR_INDEX_KEY);
        if (allIds == null || allIds.isEmpty()) {
            log.debug("向量索引为空");
            return results;
        }

        // 遍历计算相似度
        for (String id : allIds) {
            String[] parts = id.split(":");
            if (parts.length != 2) {
                continue;
            }

            String type = parts[0];
            Long productId;

            try {
                productId = Long.parseLong(parts[1]);
            } catch (NumberFormatException e) {
                continue;
            }

            // 类型过滤
            if (productType != null && !productType.equals(type)) {
                continue;
            }

            // 获取向量并计算相似度
            float[] embedding = getVector(productId, type);
            if (embedding == null) {
                continue;
            }

            float similarity = calculateCosineSimilarity(queryEmbedding, embedding);

            // 阈值过滤
            if (similarity >= threshold) {
                Map<String, String> metadata = getMetadata(productId, type);
                results.add(new SearchResult(productId, type, similarity, metadata));
            }
        }

        // 按相似度降序排序，取 topK
        results.sort((a, b) -> Float.compare(b.getSimilarity(), a.getSimilarity()));

        if (results.size() > topK) {
            results = results.subList(0, topK);
        }

        log.info("语义搜索完成: 候选={}, 结果={}", allIds.size(), results.size());
        return results;
    }

    /**
     * 清除所有向量数据
     */
    public void clearAll() {
        Set<String> allIds = redisTemplate.opsForSet().members(VECTOR_INDEX_KEY);
        if (allIds != null) {
            for (String id : allIds) {
                String[] parts = id.split(":");
                if (parts.length == 2) {
                    String key = buildVectorKey(parts[0], Long.parseLong(parts[1]));
                    String metaKey = VECTOR_META_KEY_PREFIX + parts[0] + ":" + parts[1];
                    redisTemplate.delete(key);
                    redisTemplate.delete(metaKey);
                }
            }
        }
        redisTemplate.delete(VECTOR_INDEX_KEY);
        redisTemplate.delete(VECTOR_VERSION_KEY);
        log.info("所有向量数据已清除");
    }

    /**
     * 更新索引版本号（用于触发重新索引）
     */
    public void updateVersion() {
        redisTemplate.opsForValue().set(VECTOR_VERSION_KEY, String.valueOf(System.currentTimeMillis()));
    }

    /**
     * 获取索引版本号
     */
    public String getVersion() {
        return redisTemplate.opsForValue().get(VECTOR_VERSION_KEY);
    }

    /**
     * 获取索引中的商品数量
     */
    public long getIndexCount() {
        Long size = redisTemplate.opsForSet().size(VECTOR_INDEX_KEY);
        return size != null ? size : 0;
    }

    // ==================== 私有方法 ====================

    private String buildVectorKey(String productType, Long productId) {
        return VECTOR_DATA_KEY_PREFIX + productType + ":" + productId;
    }

    private String vectorToJson(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private float[] jsonToVector(String json) {
        try {
            json = json.trim();
            if (json.startsWith("[") && json.endsWith("]")) {
                json = json.substring(1, json.length() - 1);
                String[] parts = json.split(",");
                float[] vector = new float[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    vector[i] = Float.parseFloat(parts[i].trim());
                }
                return vector;
            }
        } catch (Exception e) {
            log.error("解析向量数据失败", e);
        }
        return null;
    }

    private float calculateCosineSimilarity(float[] vec1, float[] vec2) {
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

    // ==================== 数据类 ====================

    /**
     * 向量数据
     */
    public static class VectorData {
        private Long productId;
        private String productType;
        private float[] embedding;
        private Map<String, String> metadata;

        public VectorData() {}

        public VectorData(Long productId, String productType, float[] embedding, Map<String, String> metadata) {
            this.productId = productId;
            this.productType = productType;
            this.embedding = embedding;
            this.metadata = metadata;
        }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductType() { return productType; }
        public void setProductType(String productType) { this.productType = productType; }
        public float[] getEmbedding() { return embedding; }
        public void setEmbedding(float[] embedding) { this.embedding = embedding; }
        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    }

    /**
     * 搜索结果
     */
    public static class SearchResult {
        private Long productId;
        private String productType;
        private float similarity;
        private Map<String, String> metadata;

        public SearchResult(Long productId, String productType, float similarity, Map<String, String> metadata) {
            this.productId = productId;
            this.productType = productType;
            this.similarity = similarity;
            this.metadata = metadata;
        }

        public Long getProductId() { return productId; }
        public String getProductType() { return productType; }
        public float getSimilarity() { return similarity; }
        public Map<String, String> getMetadata() { return metadata; }
    }
}
