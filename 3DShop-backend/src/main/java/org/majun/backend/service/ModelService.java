package org.majun.backend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.majun.backend.dto.CategoryCreateRequest;
import org.majun.backend.dto.CategoryUpdateRequest;
import org.majun.backend.dto.MaterialAddRequest;
import org.majun.backend.dto.MaterialUpdateRequest;
import org.majun.backend.dto.ModelCreateRequest;
import org.majun.backend.dto.ModelQueryRequest;
import org.majun.backend.dto.ModelUpdateRequest;
import org.majun.backend.entity.SysModelCategory;
import org.majun.backend.vo.CategoryVO;
import org.majun.backend.vo.MaterialVO;
import org.majun.backend.vo.ModelFavoriteToggleVO;
import org.majun.backend.vo.ModelDetailVO;
import org.majun.backend.vo.ModelListVO;
import org.majun.backend.vo.PageResult;

import java.util.List;

/**
 * 模型服务接口
 */
public interface ModelService {

    /**
     * 分页查询模型列表
     */
    PageResult<?> getModelList(ModelQueryRequest queryRequest);

    /**
     * 根据ID查询模型详情
     */
    ModelDetailVO getModelDetail(Long id);

    
    // ==================== 模型增删改 ====================

    /**
     * 创建模型
     */
    Long createModel(ModelCreateRequest request, Long designerId);

    /**
     * 更新模型
     */
    void updateModel(ModelUpdateRequest request, Long designerId);

    /**
     * 删除模型（逻辑删除）
     */
    void deleteModel(Long id, Long designerId);

    /**
     * 获取模型的设计者ID
     */
    Long getModelDesignerId(Long id);

    // ==================== 分类管理 ====================

    /**
     * 查询分类树形结构（支持按分类名查询）
     */
    IPage<?> getCategoryTree(Long parentId, String categoryName, Integer status, Integer pageNum, Integer pageSize);

    /**
     * 递归查询完整的分类树（包含所有子分类）
     */
    List<CategoryVO> getCategoryTreeRecursive(Long parentId, String categoryName);

    /**
     * 根据ID查询分类
     */
    SysModelCategory getCategoryById(Long id);

    /**
     * 创建分类
     */
    Long createCategory(CategoryCreateRequest request);

    /**
     * 更新分类
     */
    void updateCategory(CategoryUpdateRequest request);

    /**
     * 删除分类（逻辑删除）
     */
    void deleteCategory(Long id);

    // ==================== 模型图片管理 ====================

    /**
     * 添加模型图片
     */
    Long addModelImage(Long modelId, String imageUrl, Integer isMain, Integer imgType, Integer sortOrder);

    /**
     * 设置主图
     */
    void setMainImage(Long modelId, Long imageId);

    /**
     * 删除模型图片
     */
    void deleteModelImage(Long imageId);

    /**
     * 更新图片排序
     */
    void updateImageSort(Long imageId, Integer sortOrder);

    // ==================== 模型材质管理 ====================

    /**
     * 为模型添加材质
     */
    void addMaterialToModel(Long modelId, MaterialAddRequest request);

    /**
     * 删除模型的材质
     */
    void deleteMaterialFromModel(Long modelId, Long materialId);

    /**
     * 获取模型的材质列表
     */
    List<MaterialVO> getModelMaterials(Long modelId);

    /**
     * 更新模型材质
     */
    void updateMaterialInModel(Long modelId, MaterialUpdateRequest request);

    /**
     * 切换模型收藏状态
     */
    ModelFavoriteToggleVO toggleFavorite(Long modelId, Long userId);

    /**
     * 分页查询我的收藏模型
     */
    PageResult<ModelListVO> getMyFavoriteModels(Integer pageNum, Integer pageSize, Long userId);

    /**
     * 查询我的收藏模型ID列表
     */
    List<Long> getMyFavoriteModelIds(Long userId);
}
