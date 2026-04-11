package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.dto.CategoryCreateRequest;
import org.majun.backend.dto.CategoryUpdateRequest;
import org.majun.backend.dto.MaterialAddRequest;
import org.majun.backend.dto.MaterialUpdateRequest;
import org.majun.backend.dto.ModelCreateRequest;
import org.majun.backend.dto.ModelQueryRequest;
import org.majun.backend.dto.ModelUpdateRequest;
import org.majun.backend.entity.SysModel;
import org.majun.backend.entity.SysModelFavorite;
import org.majun.backend.entity.SysModelImage;
import org.majun.backend.entity.SysMaterial;
import org.majun.backend.entity.SysModelCategory;
import org.majun.backend.entity.SysUser;
import org.majun.backend.entity.ModelMaterial;
import org.majun.backend.entity.ModelImageWatermark;
import org.majun.backend.entity.SysOrderComment;
import org.majun.backend.repository.SysModelRepository;
import org.majun.backend.repository.SysModelFavoriteRepository;
import org.majun.backend.repository.SysModelImageRepository;
import org.majun.backend.repository.SysMaterialRepository;
import org.majun.backend.repository.SysModelCategoryRepository;
import org.majun.backend.repository.SysUserRepository;
import org.majun.backend.repository.ModelMaterialRepository;
import org.majun.backend.repository.ModelImageWatermarkRepository;
import org.majun.backend.repository.SysOrderCommentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.majun.backend.service.ModelService;
import org.majun.backend.service.ModelDownloadService;
import org.majun.backend.service.ImageWatermarkService;
import org.majun.backend.util.RedisUtil;
import org.majun.backend.vo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 模型服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
/**
 * 模型服务实现
 */
public class ModelServiceImpl extends ServiceImpl<SysModelRepository, SysModel> implements ModelService {

    private final SysModelRepository modelRepository;
    private final SysModelFavoriteRepository modelFavoriteRepository;
    private final SysModelImageRepository modelImageRepository;
    private final SysMaterialRepository materialRepository;
    private final SysModelCategoryRepository categoryRepository;
    private final SysUserRepository userRepository;
    private final RedisUtil redisUtil;
    private final ModelMaterialRepository modelMaterialRepository;
    private final ModelDownloadService modelDownloadService;
    private final ImageWatermarkService imageWatermarkService;
    private final ModelImageWatermarkRepository watermarkRepository;
    private final SysOrderCommentRepository orderCommentRepository;

    @Override
    public PageResult<?> getModelList(ModelQueryRequest queryRequest) {
        // 构建缓存键
        String cacheKey = buildModelListCacheKey(queryRequest);

        // 尝试从缓存获取
        String cachedData = redisUtil.getModelList(cacheKey);
        if (cachedData != null) {
            try {
                return convertJsonToPageResult(cachedData);
            } catch (Exception e) {
                log.error("缓存数据转换失败，重新查询", e);
            }
        }

        LambdaQueryWrapper<SysModel> queryWrapper = new LambdaQueryWrapper<>();

        // 分类筛选 - 添加分类有效性检查
        if (queryRequest.getCategoryId() != null) {
            SysModelCategory category = categoryRepository.selectById(queryRequest.getCategoryId());
            if (category == null || category.getIsDelete() == 1) {
                // 分类不存在或已删除，返回空结果
                return PageResult.<ModelListVO>builder()
                        .records(Collections.emptyList())
                        .total(0L)
                        .pageNum(queryRequest.getPageNum())
                        .pageSize(queryRequest.getPageSize())
                        .pages(0)
                        .build();
            }
            queryWrapper.eq(SysModel::getCategoryId, queryRequest.getCategoryId());
        }

        // 模型名称模糊搜索
        if (StringUtils.hasText(queryRequest.getModelName())) {
            queryWrapper.like(SysModel::getModelName, queryRequest.getModelName());
        }

        // 设计者筛选
        if (queryRequest.getDesignerId() != null) {
            queryWrapper.eq(SysModel::getDesignerId, queryRequest.getDesignerId());
        }

        // 上架状态筛选
        if (queryRequest.getStatus() != null) {
            queryWrapper.eq(SysModel::getStatus, queryRequest.getStatus());
        }

        // 排序
        if (StringUtils.hasText(queryRequest.getOrderBy())) {
            switch (queryRequest.getOrderBy()) {
                case "create_time" -> queryWrapper.orderByDesc(SysModel::getCreateTime);
                case "create_time_asc" -> queryWrapper.orderByAsc(SysModel::getCreateTime);
                case "price_asc" -> queryWrapper.orderByAsc(SysModel::getBasePrice);
                case "price_desc" -> queryWrapper.orderByDesc(SysModel::getBasePrice);
                case "sales" -> queryWrapper.orderByDesc(SysModel::getDownloadCount);
                case "sales_asc" -> queryWrapper.orderByAsc(SysModel::getDownloadCount);
                case "score" -> {
                    // 评分排序需要子查询，使用 last() 添加子查询
                    queryWrapper.last("ORDER BY (SELECT COALESCE(AVG(model_score), 0) FROM sys_order_comment WHERE model_id = sys_model.id AND status = 1) DESC");
                }
                case "score_asc" -> {
                    queryWrapper.last("ORDER BY (SELECT COALESCE(AVG(model_score), 0) FROM sys_order_comment WHERE model_id = sys_model.id AND status = 1) ASC");
                }
                case null, default -> queryWrapper.orderByDesc(SysModel::getCreateTime);
            }
        } else {
            queryWrapper.orderByDesc(SysModel::getCreateTime);
        }

        // 逻辑删除过滤
        queryWrapper.eq(SysModel::getIsDelete, 0);

        // 2. 分页查询
        Page<SysModel> page = new Page<>(queryRequest.getPageNum(), queryRequest.getPageSize());
        modelRepository.selectPage(page, queryWrapper);

        // 3. 查询每张模型的主图、水印图和缩略图
        Map<Long, String> modelMainImages = new HashMap<>();
        Map<Long, String> modelWatermarkedImages = new HashMap<>();
        Map<Long, String> modelThumbnails = new HashMap<>();

        for (SysModel model : page.getRecords()) {
            List<SysModelImage> images = modelImageRepository.selectList(
                new LambdaQueryWrapper<SysModelImage>()
                    .eq(SysModelImage::getModelId, model.getId())
                    .eq(SysModelImage::getIsMain, 1)
                    .last("LIMIT 1")
            );
            if (images != null && !images.isEmpty()) {
                SysModelImage mainImage = images.get(0);
                modelMainImages.put(model.getId(), mainImage.getImageUrl());

                // 查询水印图和缩略图
                String watermarkedUrl = imageWatermarkService.getWatermarkedUrl(model.getId(), mainImage.getId());
                String thumbnailUrl = imageWatermarkService.getThumbnailUrl(model.getId(), mainImage.getId());

                modelWatermarkedImages.put(model.getId(), watermarkedUrl);
                modelThumbnails.put(model.getId(), thumbnailUrl);
            } else {
                modelMainImages.put(model.getId(), model.getFilePath());
            }
        }

        // 3. 潬换为VO
        Set<Long> categoryIds = page.getRecords().stream()
                .map(SysModel::getCategoryId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> categoryNameMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(categoryIds)) {
            List<SysModelCategory> categories = categoryRepository.selectBatchIds(categoryIds);
            for (SysModelCategory category : categories) {
                if (category != null && category.getIsDelete() != null && category.getIsDelete() == 0) {
                    categoryNameMap.put(category.getId(), category.getCategoryName());
                }
            }
        }

        List<ModelListVO> records = page.getRecords().stream().map(model -> {
            ModelListVO vo = new ModelListVO();
            vo.setId(model.getId());
            vo.setModelName(model.getModelName());
            vo.setMainImageUrl(modelMainImages.get(model.getId()));
            vo.setWatermarkedMainImageUrl(modelWatermarkedImages.get(model.getId()));
            vo.setThumbnailUrl(modelThumbnails.get(model.getId()));
            vo.setDesignerName("设计者_" + model.getDesignerId());
            vo.setBasePrice(model.getBasePrice() != null ? model.getBasePrice().toString() : "0.00");
            vo.setBaseVolume(model.getBaseVolume() != null ? model.getBaseVolume().toPlainString() : "");
            vo.setBaseSize(model.getBaseSize());
            vo.setCategoryName(categoryNameMap.get(model.getCategoryId()));
            vo.setStatus(model.getStatus());
            vo.setImageCount(0);
            vo.setDownloadCount(model.getDownloadCount() != null ? model.getDownloadCount() : 0);
            return vo;
        }).collect(Collectors.toList());

        // 4. 批量查询模型评分
        if (!records.isEmpty()) {
            List<Long> modelIds = records.stream().map(ModelListVO::getId).collect(Collectors.toList());
            Map<Long, Double> avgScoreMap = new HashMap<>();

            // 查询每个模型的平均评分
            List<SysOrderComment> comments = orderCommentRepository.selectList(
                new LambdaQueryWrapper<SysOrderComment>()
                    .in(SysOrderComment::getModelId, modelIds)
                    .eq(SysOrderComment::getStatus, 1)
            );

            // 按模型ID分组计算平均评分
            Map<Long, List<SysOrderComment>> commentByModel = comments.stream()
                .collect(Collectors.groupingBy(SysOrderComment::getModelId));

            for (Map.Entry<Long, List<SysOrderComment>> entry : commentByModel.entrySet()) {
                List<SysOrderComment> modelComments = entry.getValue();
                double avgScore = modelComments.stream()
                    .mapToInt(c -> c.getModelScore() != null ? c.getModelScore() : 0)
                    .average()
                    .orElse(0.0);
                avgScoreMap.put(entry.getKey(), Math.round(avgScore * 10) / 10.0); // 保留一位小数
            }

            // 填充评分到VO
            for (ModelListVO vo : records) {
                vo.setAvgScore(avgScoreMap.getOrDefault(vo.getId(), 0.0));
            }
        }

        // 5. 构建返回结果
        PageResult<ModelListVO> result = PageResult.<ModelListVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();

        // 缓存结果
        String jsonData = convertPageResultToJson(result);
        redisUtil.setModelList(cacheKey, jsonData, RedisUtil.CACHE_EXPIRE_SECONDS);

        return result;
    }

    /**
     * 将JSON字符串转换为PageResult对象
     */
    private PageResult<ModelListVO> convertJsonToPageResult(String json) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, new TypeReference<>() {
        });
    }

    @Override
    public ModelDetailVO getModelDetail(Long id) {
        return getModelDetail(id, null);
    }

    @Override
    public ModelDetailVO getModelDetail(Long id, Long userId) {
        // 1. 查询模型基本信息
        SysModel model = modelRepository.selectById(id);
        if (model == null || model.getIsDelete() == 1) {
            throw new RuntimeException("模型不存在或已删除");
        }

        // 2. 查询模型图片列表
        List<SysModelImage> images = modelImageRepository.selectList(
                new LambdaQueryWrapper<SysModelImage>()
                        .eq(SysModelImage::getModelId, id)
                        .orderByAsc(SysModelImage::getSortOrder)
        );

        // 3. 潬换图片为VO
        List<ModelImageVO> imageVOList = images.stream()
                .map(this::convertToModelImageVO)
                .collect(Collectors.toList());

        // 4. 查询模型材质列表
        List<MaterialVO> materialVOList = getModelMaterials(id);

        // 5. 查询分类信息
        Long categoryId = model.getCategoryId();
        String categoryName = null;
        if (categoryId != null) {
            SysModelCategory category = categoryRepository.selectById(categoryId);
            if (category != null && category.getIsDelete() == 0) {
                categoryName = category.getCategoryName();
            }
        }

        // 6. 查询设计者信息
        SysUser designer = userRepository.selectById(model.getDesignerId());
        String designerName = designer != null ? designer.getNickname() : "未知设计者";

        // 7. 检查购买状态
        Boolean purchased = false;
        if (userId != null) {
            purchased = modelDownloadService.canDownloadModel(id, userId);
        }

        // 8. 生成预览URL
        String previewUrl = null;
        try {
            previewUrl = modelDownloadService.generatePreviewUrl(id);
        } catch (Exception e) {
            log.warn("生成预览URL失败: modelId={}", id);
        }

        // 9. 构建返回结果（不再返回filePath）
        return ModelDetailVO.builder()
                .id(model.getId())
                .modelName(model.getModelName())
                .description(model.getDescription() != null ? model.getDescription() : "")
                .categoryId(categoryId)
                .categoryName(categoryName)
                .licenseType(model.getLicenseType())
                .designerId(model.getDesignerId())
                .designerName(designerName)
                .basePrice(model.getBasePrice())
                .baseVolume(model.getBaseVolume())
                .baseSize(model.getBaseSize())
                .mainImageUrl(resolveDetailMainImageUrl(imageVOList, model.getMainImage()))
                .filePath(model.getFilePath())
                .status(model.getStatus())
                .images(imageVOList)
                .materials(materialVOList)
                .purchased(purchased)
                .previewUrl(previewUrl)
                .fileSize(model.getFileSize())
                .downloadCount(model.getDownloadCount())
                .build();
    }

    private String resolveDetailMainImageUrl(List<ModelImageVO> images, String fallbackFilePath) {
        if (!CollectionUtils.isEmpty(images)) {
            for (ModelImageVO image : images) {
                if (image != null && Integer.valueOf(1).equals(image.getIsMain()) && StringUtils.hasText(image.getImageUrl())) {
                    return image.getImageUrl();
                }
            }
            for (ModelImageVO image : images) {
                if (image != null && StringUtils.hasText(image.getImageUrl())) {
                    return image.getImageUrl();
                }
            }
        }
        return fallbackFilePath;
    }



    /**
     * 转换模型为列表VO
     */
    private ModelListVO convertToModelListVO(SysModel model) {
        // 获取主图
        String mainImageUrl = model.getMainImage();

        // 获取分类名称
        String categoryName = "";
        if (model.getCategoryId() != null) {
            SysModelCategory category = categoryRepository.selectById(model.getCategoryId());
            if (category != null && category.getIsDelete() == 0) {
                categoryName = category.getCategoryName();
            }
        }

        // 获取图片数量
        Integer imageCount = modelImageRepository.selectCount(
                new LambdaQueryWrapper<SysModelImage>()
                        .eq(SysModelImage::getModelId, model.getId())
        ).intValue();

        return ModelListVO.builder()
                .id(model.getId())
                .modelName(model.getModelName())
                .mainImageUrl(mainImageUrl)
                .designerName("设计者_" + model.getDesignerId())
                .basePrice(model.getBasePrice() != null ? model.getBasePrice().toString() : "0.00")
            .baseVolume(model.getBaseVolume() != null ? model.getBaseVolume().toPlainString() : "")
            .baseSize(model.getBaseSize())
                .categoryName(categoryName)
                .status(model.getStatus())
                .imageCount(imageCount)
                .build();
    }

    /**
     * 转换模型图片为VO
     */
    private ModelImageVO convertToModelImageVO(SysModelImage image) {
        // 查找水印URL
        String watermarkedUrl = null;
        try {
            watermarkedUrl = modelDownloadService.getWatermarkedImageUrl(image.getModelId(), image.getId());
            if (watermarkedUrl != null) {
                log.debug("图片水印URL: imageId={}, watermarkedUrl={}", image.getId(), watermarkedUrl);
            }
        } catch (Exception e) {
            log.warn("查询水印URL失败: imageId={}, error={}", image.getId(), e.getMessage());
        }

        return ModelImageVO.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .watermarkedUrl(watermarkedUrl)
                .isMain(image.getIsMain())
                .imgType(image.getImgType())
                .sortOrder(image.getSortOrder())
                .build();
    }

    /**
     * 转换材质为VO
     */
    private MaterialVO convertToMaterialVO(SysMaterial material) {
        return MaterialVO.builder()
                .id(material.getId())
                .name(material.getName())
                .price(material.getPrice() != null ? material.getPrice() : BigDecimal.ZERO)
                .build();
    }

    // ==================== 模型增删改实现 ====================

    @Override
    public Long createModel(ModelCreateRequest request, Long designerId) {
        SysModel model = new SysModel();
        model.setModelName(request.getModelName());
        model.setDescription(request.getDescription());
        model.setCategoryId(request.getCategoryId());
        model.setBasePrice(request.getBasePrice());
        model.setBaseVolume(request.getBaseVolume());
        model.setBaseSize(request.getBaseSize());
        model.setFilePath(request.getFilePath());
        model.setLicenseType(request.getLicenseType() != null ? request.getLicenseType() : "Commercial");
        model.setDesignerId(designerId);
        model.setStatus(request.getStatus() != null ? request.getStatus() : 0); // 默认为审核中
        model.setIsDelete(0);

        modelRepository.insert(model);
        log.info("创建模型成功, modelId: {}", model.getId());

        // 如果提供了主图URL，创建主图记录
        if (StringUtils.hasText(request.getMainImageUrl())) {
            SysModelImage mainImage = new SysModelImage();
            mainImage.setModelId(model.getId());
            mainImage.setImageUrl(request.getMainImageUrl());
            mainImage.setIsMain(1);
            mainImage.setImgType(1);
            mainImage.setSortOrder(0);
            modelImageRepository.insert(mainImage);
            log.info("创建模型主图成功, modelId: {}", model.getId());
        }

        // 清除模型列表缓存
        clearAllModelCache();

        return model.getId();
    }

    @Override
    public void updateModel(ModelUpdateRequest request, Long designerId) {
        // 1. 查询模型
        SysModel model = modelRepository.selectById(request.getId());
        if (model == null || model.getIsDelete() == 1) {
            throw new RuntimeException("模型不存在或已删除");
        }

        // 2. 更新字段
        if (StringUtils.hasText(request.getModelName())) {
            model.setModelName(request.getModelName());
        }
        if (request.getCategoryId() != null) {
            model.setCategoryId(request.getCategoryId());
        }
        if (request.getBasePrice() != null) {
            model.setBasePrice(request.getBasePrice());
        }
        if (request.getBaseVolume() != null) {
            model.setBaseVolume(request.getBaseVolume());
        }
        if (StringUtils.hasText(request.getBaseSize())) {
            model.setBaseSize(request.getBaseSize());
        }
        if (StringUtils.hasText(request.getFilePath())) {
            model.setFilePath(request.getFilePath());
        }
        if (request.getDescription() != null) {
            model.setDescription(request.getDescription());
        }
        if (request.getMainImageUrl() != null) {
            // 更新主图URL - 首先查找现有的主图记录
            SysModelImage existingMainImage = modelImageRepository.selectOne(
                    new LambdaQueryWrapper<SysModelImage>()
                            .eq(SysModelImage::getModelId, model.getId())
                            .eq(SysModelImage::getIsMain, 1)
            );
            if (existingMainImage != null) {
                // 更新现有主图
                existingMainImage.setImageUrl(request.getMainImageUrl());
                modelImageRepository.updateById(existingMainImage);
            } else {
                // 创建新的主图记录
                SysModelImage newMainImage = new SysModelImage();
                newMainImage.setModelId(model.getId());
                newMainImage.setImageUrl(request.getMainImageUrl());
                newMainImage.setIsMain(1);
                newMainImage.setImgType(1);
                newMainImage.setSortOrder(0);
                modelImageRepository.insert(newMainImage);
            }
        }
        if (request.getLicenseType() != null) {
            model.setLicenseType(request.getLicenseType());
        }
        if (request.getStatus() != null) {
            model.setStatus(request.getStatus());
        }

        // 3. 更新数据库
        modelRepository.updateById(model);
        log.info("更新模型成功, modelId: {}", model.getId());

        // 4. 清除模型列表缓存
        clearAllModelCache();
    }

    @Override
    public void deleteModel(Long id, Long designerId) {
        // 1. 查询模型
        SysModel model = modelRepository.selectById(id);
        if (model == null || model.getIsDelete() == 1) {
            throw new RuntimeException("模型不存在或已删除");
        }

        // 2. 逻辑删除
        model.setIsDelete(1);
        modelRepository.updateById(model);
        log.info("删除模型成功, modelId: {}", id);

        // 3. 清除模型列表缓存
        clearAllModelCache();
    }

    @Override
    public Long getModelDesignerId(Long id) {
        SysModel model = modelRepository.selectById(id);
        if (model == null || model.getIsDelete() == 1) {
            throw new RuntimeException("模型不存在或已删除");
        }
        return model.getDesignerId();
    }

    // ==================== 分类管理实现 ====================

    @Override
    public IPage<org.majun.backend.entity.SysModelCategory> getCategoryTree(Long parentId,
                                                                            String categoryName,
                                                                            Integer status,
                                                                            Integer pageNum,
                                                                            Integer pageSize) {
        // 构建缓存键
        String cacheKey = "category:tree:";
        if (parentId != null) {
            cacheKey += "parent:" + parentId + ":";
        } else {
            cacheKey += "all:";
        }
        if (StringUtils.hasText(categoryName)) {
            cacheKey += "name:" + categoryName + ":";
        }
        if (status != null) {
            cacheKey += "status:" + status + ":";
        }
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        cacheKey += "page:" + currentPage + ":size:" + currentSize;

        // 尝试从缓存获取
        String cachedData = redisUtil.getCategoryTree(cacheKey);
        if (cachedData != null) {
            try {
                return convertJsonToIPage(cachedData);
            } catch (Exception e) {
                log.error("分类树缓存数据转换失败，重新查询", e);
            }
        }

        LambdaQueryWrapper<SysModelCategory> queryWrapper = new LambdaQueryWrapper<SysModelCategory>()
                .eq(SysModelCategory::getIsDelete, 0);

        // 如果指定了parentId，则查询该父级下分类；否则查询全部分类
        if (parentId != null) {
            queryWrapper.eq(SysModelCategory::getParentId, parentId);
        }

        // 如果指定了分类名，则进行模糊查询
        if (StringUtils.hasText(categoryName)) {
            queryWrapper.like(SysModelCategory::getCategoryName, categoryName);
        }

        if (status != null) {
            queryWrapper.eq(SysModelCategory::getStatus, status);
        }

        queryWrapper.orderByAsc(SysModelCategory::getSortNo).orderByDesc(SysModelCategory::getCreateTime);
        IPage<SysModelCategory> page = categoryRepository.selectPage(new Page<>(currentPage, currentSize), queryWrapper);

        List<SysModelCategory> categoryRecords = page.getRecords();
        if (!CollectionUtils.isEmpty(categoryRecords)) {
            Set<Long> parentIds = categoryRecords.stream()
                    .map(SysModelCategory::getParentId)
                    .filter(parentIdValue -> parentIdValue != null && parentIdValue > 0)
                    .collect(Collectors.toSet());
            Map<Long, String> parentNameMap = new HashMap<>();
            if (!CollectionUtils.isEmpty(parentIds)) {
                List<SysModelCategory> parentCategories = categoryRepository.selectBatchIds(parentIds);
                for (SysModelCategory parentCategory : parentCategories) {
                    if (parentCategory != null && parentCategory.getIsDelete() != null && parentCategory.getIsDelete() == 0) {
                        parentNameMap.put(parentCategory.getId(), parentCategory.getCategoryName());
                    }
                }
            }
            for (SysModelCategory category : categoryRecords) {
                Long parentIdValue = category.getParentId();
                if (parentIdValue == null || parentIdValue <= 0) {
                    category.setParentName(null);
                } else {
                    category.setParentName(parentNameMap.get(parentIdValue));
                }
            }
        }

        // 缓存结果
        String jsonData = convertIPageToJson(page);
        redisUtil.setCategoryTree(cacheKey, jsonData, RedisUtil.CACHE_EXPIRE_SECONDS);

        return page;
    }

    @Override
    public List<CategoryVO> getCategoryTreeRecursive(Long parentId, String categoryName) {
        // 查询所有未删除的分类
        LambdaQueryWrapper<SysModelCategory> queryWrapper = new LambdaQueryWrapper<SysModelCategory>()
                .eq(SysModelCategory::getIsDelete, 0);

        // 如果指定了分类名，则进行模糊查询
        if (StringUtils.hasText(categoryName)) {
            queryWrapper.like(SysModelCategory::getCategoryName, categoryName);
        }

        queryWrapper.orderByAsc(SysModelCategory::getSortNo);
        List<SysModelCategory> allCategories = categoryRepository.selectList(queryWrapper);

        // 构建树形结构
        return buildCategoryTree(allCategories, parentId != null ? parentId : 0L);
    }

    /**
     * 递归构建分类树
     */
    private List<CategoryVO> buildCategoryTree(List<SysModelCategory> allCategories, Long parentId) {
        List<CategoryVO> result = new java.util.ArrayList<>();

        for (SysModelCategory category : allCategories) {
            if (category.getParentId().equals(parentId)) {
                CategoryVO vo = convertToCategoryVO(category);
                // 递归查找子分类
                vo.setChildren(buildCategoryTree(allCategories, category.getId()));
                result.add(vo);
            }
        }

        return result;
    }

    /**
     * 转换分类实体为VO
     */
    private CategoryVO convertToCategoryVO(SysModelCategory category) {
        return CategoryVO.builder()
                .id(category.getId())
                .categoryName(category.getCategoryName())
                .categoryCode(category.getCategoryCode())
                .icon(category.getIcon())
                .parentId(category.getParentId())
                .sortNo(category.getSortNo())
                .status(category.getStatus())
                .createTime(category.getCreateTime() != null ? category.getCreateTime().toString() : null)
                .children(new java.util.ArrayList<>())
                .build();
    }

    /**
     * 将JSON字符串转换为IPage对象
     */
    private IPage<SysModelCategory> convertJsonToIPage(String json) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, new TypeReference<>() {
        });
    }

    @Override
    public SysModelCategory getCategoryById(Long id) {
        SysModelCategory category = categoryRepository.selectById(id);
        if (category == null || category.getIsDelete() == 1) {
            throw new RuntimeException("分类不存在或已删除");
        }
        return category;
    }

    @Override
    public Long createCategory(CategoryCreateRequest request) {
        // 1. 验证分类编码唯一性
        LambdaQueryWrapper<SysModelCategory> codeWrapper = new LambdaQueryWrapper<SysModelCategory>()
                .eq(SysModelCategory::getCategoryCode, request.getCategoryCode())
                .eq(SysModelCategory::getIsDelete, 0);
        Long count = categoryRepository.selectCount(codeWrapper);
        if (count > 0) {
            throw new RuntimeException("分类编码已存在");
        }

        // 2. 创建分类
        SysModelCategory category = new SysModelCategory();
        category.setCategoryName(request.getCategoryName());
        category.setCategoryCode(request.getCategoryCode());
        category.setIcon(request.getIcon());
        category.setParentId(request.getParentId() != null && request.getParentId() > 0 ? request.getParentId() : 0);
        category.setSortNo(request.getSortNo() != null ? request.getSortNo() : 0);
        category.setStatus(request.getStatus() != null ? request.getStatus() : 1); // 默认启用
        category.setIsDelete(0);

        categoryRepository.insert(category);
        log.info("创建分类成功, categoryId: {}", category.getId());

        // 清除分类缓存
        redisUtil.deleteAllCategories();

        return category.getId();
    }

    @Override
    public void updateCategory(CategoryUpdateRequest request) {
        // 1. 查询分类
        SysModelCategory category = categoryRepository.selectById(request.getId());
        if (category == null || category.getIsDelete() == 1) {
            throw new RuntimeException("分类不存在或已删除");
        }

        // 2. 验证分类编码唯一性（排除自己）
        if (StringUtils.hasText(request.getCategoryCode())) {
            LambdaQueryWrapper<SysModelCategory> codeWrapper = new LambdaQueryWrapper<SysModelCategory>()
                    .eq(SysModelCategory::getCategoryCode, request.getCategoryCode())
                    .eq(SysModelCategory::getIsDelete, 0)
                    .ne(SysModelCategory::getId, request.getId());
            Long count = categoryRepository.selectCount(codeWrapper);
            if (count > 0) {
                throw new RuntimeException("分类编码已存在");
            }
        }

        // 3. 更新字段
        if (StringUtils.hasText(request.getCategoryName())) {
            category.setCategoryName(request.getCategoryName());
        }
        if (StringUtils.hasText(request.getCategoryCode())) {
            category.setCategoryCode(request.getCategoryCode());
        }
        if (request.getIcon() != null) {
            category.setIcon(request.getIcon());
        }
        if (request.getParentId() != null) {
            category.setParentId(request.getParentId() > 0 ? request.getParentId() : null);
        }
        if (request.getSortNo() != null) {
            category.setSortNo(request.getSortNo());
        }
        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }

        // 4. 更新数据库
        categoryRepository.updateById(category);
        log.info("更新分类成功, categoryId: {}", category.getId());

        // 5. 清除分类缓存
        redisUtil.deleteAllCategories();
    }

    @Override
    public void deleteCategory(Long id) {
        // 1. 查询分类
        SysModelCategory category = categoryRepository.selectById(id);
        if (category == null || category.getIsDelete() == 1) {
            throw new RuntimeException("分类不存在或已删除");
        }

        // 2. 检查是否有子分类
        LambdaQueryWrapper<SysModelCategory> childWrapper = new LambdaQueryWrapper<SysModelCategory>()
                .eq(SysModelCategory::getParentId, id)
                .eq(SysModelCategory::getIsDelete, 0);
        Long childCount = categoryRepository.selectCount(childWrapper);
        if (childCount > 0) {
            throw new RuntimeException("该分类下存在子分类，无法删除");
        }

        // 3. 检查是否有模型使用该分类
        LambdaQueryWrapper<SysModel> modelWrapper = new LambdaQueryWrapper<SysModel>()
                .eq(SysModel::getCategoryId, id)
                .eq(SysModel::getIsDelete, 0);
        Long modelCount = modelRepository.selectCount(modelWrapper);
        if (modelCount > 0) {
            throw new RuntimeException("该分类下存在模型，无法删除");
        }

        // 4. 逻辑删除
        category.setIsDelete(1);
        categoryRepository.updateById(category);

        // 5. 清除分类相关缓存
        redisUtil.deleteAllCategories();
        log.info("删除分类成功, categoryId: {}", id);
    }

    /**
     * 构建模型列表缓存键
     */
    private String buildModelListCacheKey(ModelQueryRequest request) {
        StringBuilder cacheKey = new StringBuilder("model:list:");

        // 添加分类ID
        if (request.getCategoryId() != null) {
            cacheKey.append("category:").append(request.getCategoryId()).append(":");
        }

        // 添加模型名称
        if (StringUtils.hasText(request.getModelName())) {
            cacheKey.append("name:").append(request.getModelName()).append(":");
        }

        // 添加设计者ID
        if (request.getDesignerId() != null) {
            cacheKey.append("designer:").append(request.getDesignerId()).append(":");
        }

        // 添加状态
        if (request.getStatus() != null) {
            cacheKey.append("status:").append(request.getStatus()).append(":");
        }

        // 添加排序
        if (StringUtils.hasText(request.getOrderBy())) {
            cacheKey.append("order:").append(request.getOrderBy()).append(":");
        }

        return cacheKey.toString();
    }

    /**
     * 清除模型列表缓存
     */
    private void clearModelListCache(ModelQueryRequest request) {
        String cacheKey = buildModelListCacheKey(request);
        redisUtil.deleteModelList(cacheKey);
    }

    /**
     * 将PageResult转换为JSON字符串
     */
    private String convertPageResultToJson(PageResult<?> pageResult) {
        // 使用Jackson进行JSON转换
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(pageResult);
        } catch (JsonProcessingException e) {
            log.error("转换PageResult到JSON失败", e);
            // 返回包含正确结构的空结果
            return "{\"records\":[],\"total\":0,\"pageNum\":1,\"pageSize\":1000,\"pages\":0}";
        }
    }

    /**
     * 将IPage转换为JSON字符串
     */
    private String convertIPageToJson(IPage<?> page) {
        // 使用Jackson进行JSON转换
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(page);
        } catch (JsonProcessingException e) {
            log.error("转换IPage到JSON失败", e);
            // 返回包含正确结构的空结果
            return "{\"records\":[],\"total\":0,\"pageNum\":1,\"pageSize\":1000,\"pages\":0}";
        }
    }

    /**
     * 清除所有模型缓存
     */
    private void clearAllModelCache() {
        redisUtil.deleteAllModelLists();
    }

    // ==================== 模型图片管理实现 ====================

    @Override
    public Long addModelImage(Long modelId, String imageUrl, Integer isMain, Integer imgType, Integer sortOrder) {
        // 验证模型是否存在
        SysModel model = modelRepository.selectById(modelId);
        if (model == null || model.getIsDelete() == 1) {
            throw new RuntimeException("模型不存在或已删除");
        }

        // 如果设置为主图，先将该模型的其他主图取消
        if (isMain != null && isMain == 1) {
            LambdaQueryWrapper<SysModelImage> wrapper = new LambdaQueryWrapper<SysModelImage>()
                    .eq(SysModelImage::getModelId, modelId)
                    .eq(SysModelImage::getIsMain, 1);
            modelImageRepository.selectList(wrapper).forEach(img -> {
                img.setIsMain(0);
                modelImageRepository.updateById(img);
            });
        }

        // 创建新图片记录
        SysModelImage image = new SysModelImage();
        image.setModelId(modelId);
        // 确保 image_url 不为 null，使用空字符串作为默认值
        String finalImageUrl = imageUrl != null ? imageUrl : "";
        if (finalImageUrl.isEmpty()) {
            log.warn("图片URL为空，使用默认值");
            finalImageUrl = "default-image-url"; // 使用一个默认值
        }
        image.setImageUrl(finalImageUrl);
        image.setIsMain(isMain != null ? isMain : 0);
        image.setImgType(imgType != null ? imgType : 1);
        image.setSortOrder(sortOrder != null ? sortOrder : 0);

        modelImageRepository.insert(image);
        log.info("添加模型图片成功, imageId: {}, modelId: {}", image.getId(), modelId);

        // 自动生成水印（异步处理，不阻塞主流程）
        try {
            imageWatermarkService.getOrAddWatermark(modelId, image.getId(), finalImageUrl);
            log.info("模型图片水印生成成功, imageId: {}", image.getId());
        } catch (Exception e) {
            log.warn("模型图片水印生成失败, imageId: {}, error: {}", image.getId(), e.getMessage());
            // 水印生成失败不影响图片添加
        }

        // 清除模型缓存
        clearAllModelCache();

        return image.getId();
    }

    @Override
    public void setMainImage(Long modelId, Long imageId) {
        log.info("开始设置主图, modelId: {}, imageId: {}", modelId, imageId);

        // 验证参数
        if (modelId == null || imageId == null) {
            log.error("设置主图失败: 参数为空, modelId: {}, imageId: {}", modelId, imageId);
            throw new RuntimeException("参数不能为空");
        }

        // 验证模型是否存在
        SysModel model = modelRepository.selectById(modelId);
        if (model == null || model.getIsDelete() == 1) {
            log.error("设置主图失败: 模型不存在或已删除, modelId: {}", modelId);
            throw new RuntimeException("模型不存在或已删除");
        }

        // 验证图片是否存在
        SysModelImage image = modelImageRepository.selectById(imageId);
        if (image == null) {
            log.error("设置主图失败: 图片不存在, imageId: {}", imageId);
            throw new RuntimeException("图片不存在");
        }
        if (!image.getModelId().equals(modelId)) {
            log.error("设置主图失败: 图片不属于该模型, imageId: {}, modelId: {}", imageId, modelId);
            throw new RuntimeException("图片不属于该模型");
        }

        log.info("准备更新主图, modelId: {}, imageId: {}", modelId, imageId);

        // 将该模型的所有图片设置为主图
        LambdaQueryWrapper<SysModelImage> wrapper = new LambdaQueryWrapper<SysModelImage>()
                .eq(SysModelImage::getModelId, modelId);
        List<SysModelImage> allImages = modelImageRepository.selectList(wrapper);
        log.info("找到 {} 张图片需要更新", allImages.size());

        int updatedCount = 0;
        for (SysModelImage img : allImages) {
            boolean isMain = img.getId().equals(imageId);
            img.setIsMain(isMain ? 1 : 0);
            modelImageRepository.updateById(img);
            updatedCount++;
        }

        log.info("设置主图完成, 更新了 {} 张图片", updatedCount);

        // 清除模型缓存
        clearAllModelCache();
    }

    @Override
    public void deleteModelImage(Long imageId) {
        SysModelImage image = modelImageRepository.selectById(imageId);
        if (image == null) {
            throw new RuntimeException("图片不存在");
        }

        modelImageRepository.deleteById(imageId);
        log.info("删除模型图片成功, imageId: {}", imageId);

        // 清除模型缓存
        clearAllModelCache();
    }

    @Override
    public void updateImageSort(Long imageId, Integer sortOrder) {
        SysModelImage image = modelImageRepository.selectById(imageId);
        if (image == null) {
            throw new RuntimeException("图片不存在");
        }

        image.setSortOrder(sortOrder);
        modelImageRepository.updateById(image);
        log.info("更新图片排序成功, imageId: {}, sortOrder: {}", imageId, sortOrder);

        // 清除模型缓存
        clearAllModelCache();
    }

    @Override
    public void addMaterialToModel(Long modelId, MaterialAddRequest request) {
        // 检查模型是否存在
        SysModel model = modelRepository.selectById(modelId);
        if (model == null) {
            throw new RuntimeException("模型不存在");
        }

        // 材质信息以 sys_model_material 关联表为准，不依赖独立材质主表

        // 创建模型材质关联
        ModelMaterial modelMaterial = new ModelMaterial();
        modelMaterial.setModelId(modelId);
        modelMaterial.setMaterialId(request.getMaterialId());
        modelMaterial.setMaterialName(request.getName());
        modelMaterial.setPrice(request.getPrice());
        modelMaterial.setIsTrusted(request.getIsTrusted() != null ? request.getIsTrusted() : false);
        modelMaterial.setIsEco(request.getIsEco() != null ? request.getIsEco() : false);
        modelMaterial.setCreateTime(java.time.LocalDateTime.now().toString());

        modelMaterialRepository.insert(modelMaterial);

        // 前端新增自定义材质时通常不会传 materialId，这里兜底回填，避免 material_id 为空
        if (modelMaterial.getMaterialId() == null && modelMaterial.getId() != null) {
            modelMaterial.setMaterialId(modelMaterial.getId());
            modelMaterialRepository.updateById(modelMaterial);
        }

        log.info("为模型添加材质成功, modelId: {}, materialName: {}", modelId, request.getName());
    }

    @Override
    public void deleteMaterialFromModel(Long modelId, Long materialId) {
        // 检查模型是否存在
        SysModel model = modelRepository.selectById(modelId);
        if (model == null) {
            throw new RuntimeException("模型不存在");
        }

        // 删除模型材质关联
        LambdaQueryWrapper<ModelMaterial> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelMaterial::getModelId, modelId)
               .eq(ModelMaterial::getId, materialId);

        int deletedCount = modelMaterialRepository.delete(wrapper);
        if (deletedCount == 0) {
            throw new RuntimeException("材质不存在或已删除");
        }

        log.info("删除模型材质成功, modelId: {}, materialId: {}", modelId, materialId);
    }

    @Override
    public List<MaterialVO> getModelMaterials(Long modelId) {
        // 检查模型是否存在
        SysModel model = modelRepository.selectById(modelId);
        if (model == null) {
            throw new RuntimeException("模型不存在");
        }

        // 查询模型的所有材质
        LambdaQueryWrapper<ModelMaterial> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelMaterial::getModelId, modelId)
               .orderByDesc(ModelMaterial::getIsEco)  // 环保材质优先
               .orderByAsc(ModelMaterial::getCreateTime);

        List<ModelMaterial> modelMaterials = modelMaterialRepository.selectList(wrapper);

        // 转换为VO
        return modelMaterials.stream()
                .map(mm -> {
                    MaterialVO vo = new MaterialVO();
                    vo.setId(mm.getId());
                    vo.setName(mm.getMaterialName());
                    vo.setPrice(java.math.BigDecimal.valueOf(mm.getPrice()));
                    vo.setIsTrusted(mm.getIsTrusted());
                    vo.setIsEco(Boolean.TRUE.equals(mm.getIsEco()));
                    return vo;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void updateMaterialInModel(Long modelId, MaterialUpdateRequest request) {
        // 检查模型是否存在
        SysModel model = modelRepository.selectById(modelId);
        if (model == null) {
            throw new RuntimeException("模型不存在");
        }

        // 检查材质是否存在
        LambdaQueryWrapper<ModelMaterial> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelMaterial::getModelId, modelId)
               .eq(ModelMaterial::getId, request.getMaterialId());

        ModelMaterial modelMaterial = modelMaterialRepository.selectOne(wrapper);
        if (modelMaterial == null) {
            throw new RuntimeException("材质不存在或已删除");
        }

        // 更新材质信息
        modelMaterial.setMaterialName(request.getName());
        modelMaterial.setPrice(request.getPrice());
        modelMaterial.setIsTrusted(request.getIsTrusted() != null ? request.getIsTrusted() : false);
        modelMaterial.setIsEco(request.getIsEco() != null ? request.getIsEco() : false);

        int updateCount = modelMaterialRepository.updateById(modelMaterial);
        if (updateCount == 0) {
            throw new RuntimeException("材质更新失败");
        }

        log.info("更新模型材质成功, modelId: {}, materialId: {}", modelId, request.getMaterialId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelFavoriteToggleVO toggleFavorite(Long modelId, Long userId) {
        SysModel model = modelRepository.selectById(modelId);
        if (model == null || Integer.valueOf(1).equals(model.getIsDelete())) {
            throw new RuntimeException("模型不存在或已删除");
        }

        SysModelFavorite existed = modelFavoriteRepository.selectOne(new LambdaQueryWrapper<SysModelFavorite>()
                .eq(SysModelFavorite::getUserId, userId)
                .eq(SysModelFavorite::getModelId, modelId));

        boolean active;
        if (existed != null) {
            modelFavoriteRepository.deleteById(existed.getId());
            active = false;
        } else {
            SysModelFavorite favorite = new SysModelFavorite();
            favorite.setUserId(userId);
            favorite.setModelId(modelId);
            modelFavoriteRepository.insert(favorite);
            active = true;
        }
        return new ModelFavoriteToggleVO(modelId, active);
    }

    @Override
    public PageResult<ModelListVO> getMyFavoriteModels(Integer pageNum, Integer pageSize, Long userId) {
        Page<SysModelFavorite> favoritePage = new Page<>(pageNum, pageSize);
        modelFavoriteRepository.selectPage(favoritePage, new LambdaQueryWrapper<SysModelFavorite>()
                .eq(SysModelFavorite::getUserId, userId)
                .orderByDesc(SysModelFavorite::getCreateTime));

        List<Long> modelIds = favoritePage.getRecords().stream().map(SysModelFavorite::getModelId).toList();
        if (CollectionUtils.isEmpty(modelIds)) {
            return PageResult.<ModelListVO>builder()
                    .records(List.of())
                    .total(favoritePage.getTotal())
                    .pageNum((int) favoritePage.getCurrent())
                    .pageSize((int) favoritePage.getSize())
                    .pages((int) favoritePage.getPages())
                    .build();
        }

        List<SysModel> models = modelRepository.selectList(new LambdaQueryWrapper<SysModel>()
                .in(SysModel::getId, modelIds)
                .eq(SysModel::getIsDelete, 0));

        Map<Long, SysModel> modelMap = models.stream().collect(Collectors.toMap(SysModel::getId, m -> m));
        List<ModelListVO> records = new ArrayList<>();
        for (Long modelId : modelIds) {
            SysModel model = modelMap.get(modelId);
            if (model == null) {
                continue;
            }
            records.add(convertToModelListVO(model));
        }

        return PageResult.<ModelListVO>builder()
                .records(records)
                .total(favoritePage.getTotal())
                .pageNum((int) favoritePage.getCurrent())
                .pageSize((int) favoritePage.getSize())
                .pages((int) favoritePage.getPages())
                .build();
    }

    @Override
    public List<Long> getMyFavoriteModelIds(Long userId) {
        List<SysModelFavorite> favorites = modelFavoriteRepository.selectList(new LambdaQueryWrapper<SysModelFavorite>()
                .eq(SysModelFavorite::getUserId, userId)
                .orderByDesc(SysModelFavorite::getCreateTime));

        if (CollectionUtils.isEmpty(favorites)) {
            return List.of();
        }

        Set<Long> validModelIds = modelRepository.selectList(new LambdaQueryWrapper<SysModel>()
                        .in(SysModel::getId, favorites.stream().map(SysModelFavorite::getModelId).toList())
                        .eq(SysModel::getIsDelete, 0))
                .stream()
                .map(SysModel::getId)
                .collect(Collectors.toSet());

        return favorites.stream()
                .map(SysModelFavorite::getModelId)
                .filter(validModelIds::contains)
                .toList();
    }

    @Override
    public WatermarkStatusVO getWatermarkStatus(Long modelId) {
        // 获取模型信息
        SysModel model = modelRepository.selectById(modelId);
        if (model == null || model.getIsDelete() == 1) {
            throw new RuntimeException("模型不存在");
        }

        // 获取模型图片总数
        Long totalImages = modelImageRepository.selectCount(
                new LambdaQueryWrapper<SysModelImage>()
                        .eq(SysModelImage::getModelId, modelId)
        );

        // 获取已生成水印的图片数
        Long watermarkedImages = watermarkRepository.selectCount(
                new LambdaQueryWrapper<ModelImageWatermark>()
                        .eq(ModelImageWatermark::getModelId, modelId)
                        .eq(ModelImageWatermark::getIsDelete, 0)
        );

        int total = totalImages != null ? totalImages.intValue() : 0;
        int watermarked = watermarkedImages != null ? watermarkedImages.intValue() : 0;
        int coverage = total > 0 ? (watermarked * 100 / total) : 0;

        return WatermarkStatusVO.builder()
                .modelId(modelId)
                .modelName(model.getModelName())
                .totalImages(total)
                .watermarkedImages(watermarked)
                .coveragePercent(coverage)
                .isComplete(total > 0 && watermarked >= total)
                .build();
    }
}
