package org.majun.backend.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.ai.service.VectorStoreService.SearchResult;
import org.majun.backend.ai.service.VectorStoreService.VectorData;
import org.majun.backend.entity.SysModel;
import org.majun.backend.entity.SysModelImage;
import org.majun.backend.repository.SysModelRepository;
import org.majun.backend.repository.SysModelImageRepository;
import org.majun.backend.vo.ModelListVO;
import org.majun.backend.vo.PageResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 语义搜索服务
 * 提供商品/模型的语义搜索功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final SysModelRepository modelRepository;
    private final SysModelImageRepository modelImageRepository;

    /**
     * 商品类型常量
     */
    public static final String PRODUCT_TYPE_MODEL = "MODEL";

    /**
     * 默认搜索参数
     */
    private static final int DEFAULT_TOP_K = 50;
    private static final float DEFAULT_THRESHOLD = 0.3f;

    /**
     * 语义搜索模型
     *
     * @param query     搜索查询文本
     * @param pageNum   页码
     * @param pageSize  每页数量
     * @return 搜索结果
     */
    public PageResult<ModelListVO> searchModels(String query, int pageNum, int pageSize) {
        if (!embeddingService.isAvailable()) {
            log.warn("Embedding 服务不可用，返回空结果");
            return PageResult.<ModelListVO>builder()
                    .records(new ArrayList<>())
                    .total(0L)
                    .pageNum(pageNum)
                    .pageSize(pageSize)
                    .pages(0)
                    .build();
        }

        // 1. 生成查询向量
        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        if (queryEmbedding == null) {
            log.warn("生成查询向量失败");
            return PageResult.<ModelListVO>builder()
                    .records(new ArrayList<>())
                    .total(0L)
                    .pageNum(pageNum)
                    .pageSize(pageSize)
                    .pages(0)
                    .build();
        }

        // 2. 向量搜索
        int topK = Math.max(pageNum * pageSize, DEFAULT_TOP_K);
        List<SearchResult> searchResults = vectorStoreService.search(
                queryEmbedding, PRODUCT_TYPE_MODEL, topK, DEFAULT_THRESHOLD);

        if (searchResults.isEmpty()) {
            return PageResult.<ModelListVO>builder()
                    .records(new ArrayList<>())
                    .total(0L)
                    .pageNum(pageNum)
                    .pageSize(pageSize)
                    .pages(0)
                    .build();
        }

        // 3. 获取模型详情
        List<Long> modelIds = searchResults.stream()
                .map(SearchResult::getProductId)
                .collect(Collectors.toList());

        List<SysModel> models = modelRepository.selectList(
                new LambdaQueryWrapper<SysModel>()
                        .in(SysModel::getId, modelIds)
                        .eq(SysModel::getIsDelete, 0)
                        .eq(SysModel::getStatus, 1)); // 只返回已上架的模型

        if (models.isEmpty()) {
            return PageResult.<ModelListVO>builder()
                    .records(new ArrayList<>())
                    .total(0L)
                    .pageNum(pageNum)
                    .pageSize(pageSize)
                    .pages(0)
                    .build();
        }

        // 4. 构建结果映射
        Map<Long, SysModel> modelMap = models.stream()
                .collect(Collectors.toMap(SysModel::getId, m -> m));

        Map<Long, Float> similarityMap = searchResults.stream()
                .collect(Collectors.toMap(SearchResult::getProductId, SearchResult::getSimilarity));

        // 5. 获取模型图片
        Map<Long, String> mainImageMap = getModelMainImages(modelIds);

        // 6. 按相似度排序并分页
        List<ModelListVO> records = searchResults.stream()
                .filter(r -> modelMap.containsKey(r.getProductId()))
                .sorted((a, b) -> Float.compare(b.getSimilarity(), a.getSimilarity()))
                .skip((long) (pageNum - 1) * pageSize)
                .limit(pageSize)
                .map(r -> {
                    SysModel model = modelMap.get(r.getProductId());
                    ModelListVO vo = convertToVO(model, mainImageMap.get(model.getId()));
                    // 可以在 VO 中添加相似度字段用于调试或展示
                    return vo;
                })
                .collect(Collectors.toList());

        // 7. 构建分页结果
        long total = searchResults.stream()
                .filter(r -> modelMap.containsKey(r.getProductId()))
                .count();

        int pages = (int) Math.ceil((double) total / pageSize);

        return PageResult.<ModelListVO>builder()
                .records(records)
                .total(total)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .pages(pages)
                .build();
    }

    /**
     * 索引单个模型
     */
    public void indexModel(SysModel model) {
        if (model == null || model.getId() == null) {
            return;
        }

        // 构建索引文本（名称 + 描述）
        String indexText = buildIndexText(model.getModelName(), model.getDescription());

        // 生成向量
        float[] embedding = embeddingService.generateEmbedding(indexText);
        if (embedding == null) {
            log.warn("模型索引失败: modelId={}", model.getId());
            return;
        }

        // 构建元数据
        Map<String, String> metadata = new HashMap<>();
        metadata.put("name", model.getModelName() != null ? model.getModelName() : "");
        metadata.put("description", model.getDescription() != null ? model.getDescription() : "");
        if (model.getCategoryId() != null) {
            metadata.put("categoryId", String.valueOf(model.getCategoryId()));
        }

        // 存储向量
        vectorStoreService.storeVector(model.getId(), PRODUCT_TYPE_MODEL, embedding, metadata);

        log.info("模型索引成功: modelId={}, name={}", model.getId(), model.getModelName());
    }

    /**
     * 批量索引所有模型
     */
    @Async
    public void indexAllModels() {
        log.info("开始索引所有模型...");

        List<SysModel> models = modelRepository.selectList(
                new LambdaQueryWrapper<SysModel>()
                        .eq(SysModel::getIsDelete, 0)
                        .eq(SysModel::getStatus, 1));

        if (models.isEmpty()) {
            log.info("没有需要索引的模型");
            return;
        }

        // 清除旧索引
        vectorStoreService.clearAll();

        // 批量生成向量
        List<String> texts = models.stream()
                .map(m -> buildIndexText(m.getModelName(), m.getDescription()))
                .collect(Collectors.toList());

        List<float[]> embeddings = embeddingService.generateEmbeddings(texts);

        // 存储向量
        List<VectorData> vectorDataList = new ArrayList<>();
        for (int i = 0; i < models.size(); i++) {
            SysModel model = models.get(i);
            float[] embedding = i < embeddings.size() ? embeddings.get(i) : null;

            if (embedding != null) {
                Map<String, String> metadata = new HashMap<>();
                metadata.put("name", model.getModelName() != null ? model.getModelName() : "");
                metadata.put("description", model.getDescription() != null ? model.getDescription() : "");
                if (model.getCategoryId() != null) {
                    metadata.put("categoryId", String.valueOf(model.getCategoryId()));
                }

                vectorDataList.add(new VectorData(model.getId(), PRODUCT_TYPE_MODEL, embedding, metadata));
            }
        }

        vectorStoreService.storeVectors(vectorDataList);
        vectorStoreService.updateVersion();

        log.info("模型索引完成: 总数={}, 成功={}", models.size(), vectorDataList.size());
    }

    /**
     * 删除模型索引
     */
    public void deleteModelIndex(Long modelId) {
        vectorStoreService.deleteVector(modelId, PRODUCT_TYPE_MODEL);
        log.info("模型索引已删除: modelId={}", modelId);
    }

    /**
     * 获取索引状态
     */
    public IndexStatus getIndexStatus() {
        IndexStatus status = new IndexStatus();
        status.setAvailable(embeddingService.isAvailable());
        status.setIndexCount(vectorStoreService.getIndexCount());
        status.setVersion(vectorStoreService.getVersion());
        status.setDimension(embeddingService.getDimension());
        return status;
    }

    // ==================== 私有方法 ====================

    private String buildIndexText(String name, String description) {
        StringBuilder sb = new StringBuilder();
        if (name != null) {
            sb.append(name);
        }
        if (description != null && !description.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(description);
        }
        return sb.toString();
    }

    private Map<Long, String> getModelMainImages(List<Long> modelIds) {
        Map<Long, String> result = new HashMap<>();

        List<SysModelImage> images = modelImageRepository.selectList(
                new LambdaQueryWrapper<SysModelImage>()
                        .in(SysModelImage::getModelId, modelIds)
                        .eq(SysModelImage::getIsMain, 1));

        for (SysModelImage image : images) {
            if (!result.containsKey(image.getModelId())) {
                result.put(image.getModelId(), image.getImageUrl());
            }
        }

        return result;
    }

    private ModelListVO convertToVO(SysModel model, String mainImageUrl) {
        ModelListVO vo = new ModelListVO();
        vo.setId(model.getId());
        vo.setModelName(model.getModelName());
        vo.setMainImageUrl(mainImageUrl != null ? mainImageUrl : model.getMainImage());
        vo.setDesignerName("设计者_" + model.getDesignerId());
        vo.setBasePrice(model.getBasePrice() != null ? model.getBasePrice().toString() : "0.00");
        vo.setBaseVolume(model.getBaseVolume() != null ? model.getBaseVolume().toPlainString() : "");
        vo.setBaseSize(model.getBaseSize());
        vo.setStatus(model.getStatus());
        vo.setImageCount(0);
        return vo;
    }

    /**
     * 索引状态
     */
    public static class IndexStatus {
        private boolean available;
        private long indexCount;
        private String version;
        private int dimension;

        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
        public long getIndexCount() { return indexCount; }
        public void setIndexCount(long indexCount) { this.indexCount = indexCount; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public int getDimension() { return dimension; }
        public void setDimension(int dimension) { this.dimension = dimension; }
    }
}
