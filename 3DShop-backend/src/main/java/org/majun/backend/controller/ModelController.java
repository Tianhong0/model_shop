package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.majun.backend.annotation.OperationLog;
import org.majun.backend.common.Result;
import org.majun.backend.dto.*;
import org.majun.backend.security.LoginUser;
import org.majun.backend.dto.ModelAuditRequest;
import org.majun.backend.service.DataExportService;
import org.majun.backend.service.ModelService;
import org.majun.backend.vo.CategoryVO;
import org.majun.backend.vo.MaterialVO;
import org.majun.backend.vo.ModelAuditRecordVO;
import org.majun.backend.vo.ModelFavoriteToggleVO;
import org.majun.backend.vo.ModelListVO;
import org.majun.backend.vo.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 模型管理控制器
 */
@Tag(name = "模型管理", description = "模型展示相关接口")
@RestController
@RequestMapping("/api/model")
@RequiredArgsConstructor
@Validated
public class ModelController {

    private final ModelService modelService;
    private final DataExportService dataExportService;

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        // 从Spring Security上下文中获取当前用户ID
        return ((org.majun.backend.security.LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
    }

    /**
     * 分页查询模型列表
     */
    @Operation(summary = "分页查询模型列表", description = "支持按分类、名称、设计者筛选，支持分页")
    @PostMapping("/list")
    public Result<PageResult<?>> getModelList(@Valid @RequestBody ModelQueryRequest queryRequest) {
        PageResult<?> result = modelService.getModelList(queryRequest);
        return Result.success(result);
    }

    /**
     * 根据ID查询模型详情
     */
    @Operation(summary = "查询模型详情", description = "根据模型ID查询模型的详细信息，包含购买状态")
    @GetMapping("/detail/{id}")
    public Result<?> getModelDetail(@PathVariable Long id) {
        Long userId = null;
        try {
            userId = getCurrentUserId();
        } catch (Exception ignored) {
            // 未登录用户，userId为null
        }
        return Result.success(modelService.getModelDetail(id, userId));
    }

    /** 收藏模型切换 */
    @Operation(summary = "收藏模型切换", description = "再次点击取消收藏")
    @PostMapping("/favorite/toggle")
    public Result<ModelFavoriteToggleVO> toggleFavorite(@AuthenticationPrincipal LoginUser loginUser,
                                                        @Valid @RequestBody ModelFavoriteToggleRequest request) {
        return Result.success(modelService.toggleFavorite(request.getModelId(), loginUser.getId()));
    }

    /** 我的收藏模型分页 */
    @Operation(summary = "我的收藏模型分页", description = "查询当前用户收藏的模型")
    @PostMapping("/favorite/my/list")
    public Result<PageResult<?>> getMyFavoriteModels(@AuthenticationPrincipal LoginUser loginUser,
                                                     @Valid @RequestBody ModelFavoriteMyQueryRequest request) {
        return Result.success(modelService.getMyFavoriteModels(request.getPageNum(), request.getPageSize(), loginUser.getId()));
    }

    /** 我的收藏模型ID列表 */
    @Operation(summary = "我的收藏模型ID列表", description = "用于前端快速判断收藏状态")
    @GetMapping("/favorite/my/ids")
    public Result<List<Long>> getMyFavoriteModelIds(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(modelService.getMyFavoriteModelIds(loginUser.getId()));
    }

    /**
     * 查询模型分类列表（树形结构）
     */
    @Operation(summary = "查询模型分类列表", description = "获取所有模型分类，用于前端渲染分类树")
    @GetMapping("/categories")
    public Result<?> getCategoryList(@RequestParam(required = false) Long parentId,
                                     @RequestParam(required = false) String categoryName,
                                     @RequestParam(required = false) Integer status,
                                     @RequestParam(defaultValue = "1") Integer pageNum,
                                     @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(modelService.getCategoryTree(parentId, categoryName, status, pageNum, pageSize));
    }

    /**
     * 递归查询完整的分类树（包含所有子分类）
     */
    @Operation(summary = "递归查询分类树", description = "递归查询完整的分类树结构，包含所有层级的子分类")
    @GetMapping("/categories/tree")
    public Result<java.util.List<CategoryVO>> getCategoryTreeRecursive(@RequestParam(required = false) Long parentId, @RequestParam(required = false) String categoryName) {
        return Result.success(modelService.getCategoryTreeRecursive(parentId, categoryName));
    }


    /**
     * 根据ID查询分类详情
     */
    @Operation(summary = "查询分类详情", description = "根据分类ID查询分类详情")
    @GetMapping("/category/{id}")
    public Result<?> getCategoryById(@PathVariable Long id) {
        return Result.success(modelService.getCategoryById(id));
    }

    /**
     * 创建模型
     */
    @Operation(summary = "创建模型", description = "设计者创建新模型")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_DESIGNER')")
    @OperationLog(type = "CREATE", module = "模型管理", description = "创建模型", targetType = "MODEL")
    @PostMapping("/create")
    public Result<Long> createModel(@Valid @RequestBody ModelCreateRequest request) {
        Long designerId = getCurrentUserId();
        Long modelId = modelService.createModel(request, designerId);
        return Result.success(modelId);
    }

    /**
     * 更新模型
     */
    @Operation(summary = "更新模型", description = "设计者更新模型信息")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @modelServiceImpl.getModelDesignerId(#request.id) == authentication.principal.id")
    @OperationLog(type = "UPDATE", module = "模型管理", description = "更新模型", targetType = "MODEL")
    @PostMapping("/update")
    public Result<Void> updateModel(@Valid @RequestBody ModelUpdateRequest request) {
        modelService.updateModel(request, getCurrentUserId());
        return Result.success();
    }

    /**
     * 删除模型
     */
    @Operation(summary = "删除模型", description = "设计者删除模型（逻辑删除）")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @modelServiceImpl.getModelDesignerId(#id) == authentication.principal.id")
    @OperationLog(type = "DELETE", module = "模型管理", description = "删除模型", targetType = "MODEL")
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteModel(@PathVariable Long id) {
        modelService.deleteModel(id, getCurrentUserId());
        return Result.success();
    }

    /**
     * 创建分类
     */
    @Operation(summary = "创建分类", description = "创建新的模型分类")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/category/create")
    public Result<Long> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        Long categoryId = modelService.createCategory(request);
        return Result.success(categoryId);
    }

    /**
     * 更新分类
     */
    @Operation(summary = "更新分类", description = "更新模型分类信息")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/category/update")
    public Result<Void> updateCategory(@Valid @RequestBody CategoryUpdateRequest request) {
        modelService.updateCategory(request);
        return Result.success();
    }

    /**
     * 删除分类
     */
    @Operation(summary = "删除分类", description = "删除模型分类（逻辑删除）")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/category/delete/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        modelService.deleteCategory(id);
        return Result.success();
    }

    // ==================== 模型图片管理 ====================

    /**
     * 添加模型图片
     */
    @Operation(summary = "添加模型图片", description = "为模型添加新图片")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_DESIGNER')")
    @PostMapping("/image/add")
    public Result<Long> addModelImage(@RequestBody ModelImageAddRequest request) {
        Long imageId = modelService.addModelImage(request.getModelId(), request.getImageUrl(),
                                            request.getIsMain(), request.getImgType(),
                                            request.getSortOrder());
        return Result.success(imageId);
    }

    /**
     * 设置主图
     */
    @Operation(summary = "设置主图", description = "将指定图片设置为主图")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_DESIGNER')")
    @PostMapping("/image/set-main")
    public Result<Void> setMainImage(@RequestBody SetMainImageRequest request) {
        try {

            modelService.setMainImage(request.getModelId(), request.getImageId());

            return Result.success();
        } catch (Exception e) {

            throw e;
        }
    }

    /**
     * 删除模型图片
     */
    @Operation(summary = "删除模型图片", description = "删除指定的模型图片")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_DESIGNER')")
    @DeleteMapping("/image/delete/{imageId}")
    public Result<Void> deleteModelImage(@PathVariable Long imageId) {
        modelService.deleteModelImage(imageId);
        return Result.success();
    }

    /**
     * 更新图片排序
     */
    @Operation(summary = "更新图片排序", description = "更新图片的排序顺序")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_DESIGNER')")
    @PostMapping("/image/sort")
    public Result<Void> updateImageSort(@RequestBody UpdateImageSortRequest request) {
        modelService.updateImageSort(request.getImageId(), request.getSortOrder());
        return Result.success();
    }

    // ==================== 模型材质管理 ====================

    /**
     * 为模型添加材质
     */
    @Operation(summary = "添加材质", description = "为模型添加新的材质")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_DESIGNER')")
    @PostMapping("/{modelId}/material/add")
    public Result<Void> addMaterial(@PathVariable Long modelId, @RequestBody MaterialAddRequest request) {
        modelService.addMaterialToModel(modelId, request);
        return Result.success();
    }

    /**
     * 删除模型材质
     */
    @Operation(summary = "删除材质", description = "删除模型的指定材质")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_DESIGNER')")
    @DeleteMapping("/{modelId}/material/{materialId}/delete")
    public Result<Void> deleteMaterial(@PathVariable Long modelId, @PathVariable Long materialId) {
        modelService.deleteMaterialFromModel(modelId, materialId);
        return Result.success();
    }

    /**
     * 获取模型的材质列表
     */
    @Operation(summary = "获取材质列表", description = "获取模型的所有材质")
    @GetMapping("/{modelId}/materials")
    public Result<List<MaterialVO>> getModelMaterials(@PathVariable Long modelId) {
        List<MaterialVO> materials = modelService.getModelMaterials(modelId);
        return Result.success(materials);
    }

    /**
     * 更新模型材质
     */
    @Operation(summary = "更新材质", description = "更新模型的指定材质")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_DESIGNER')")
    @PostMapping("/{modelId}/material/{materialId}/update")
    public Result<Void> updateMaterial(@PathVariable Long modelId, @PathVariable Long materialId, @RequestBody MaterialUpdateRequest request) {
        // 设置材质ID到请求对象
        request.setMaterialId(materialId);
        modelService.updateMaterialInModel(modelId, request);
        return Result.success();
    }

    // ==================== 模型审核 ====================

    /**
     * 审核模型
     */
    @Operation(summary = "审核模型", description = "管理员审核设计者提交的模型")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @OperationLog(type = "AUDIT", module = "模型管理", description = "审核模型", targetType = "MODEL")
    @PostMapping("/audit")
    public Result<Void> auditModel(@Valid @RequestBody ModelAuditRequest request) {
        Long adminId = getCurrentUserId();
        modelService.auditModel(request, adminId);
        return Result.success();
    }

    /**
     * 查询模型审核记录
     */
    @Operation(summary = "查询审核记录", description = "查询模型的审核历史记录")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/audit/records/{modelId}")
    public Result<PageResult<ModelAuditRecordVO>> getAuditRecords(
            @PathVariable Long modelId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(modelService.getAuditRecords(modelId, pageNum, pageSize));
    }

    /**
     * 设计者查询自己的模型列表
     */
    @Operation(summary = "我的模型列表", description = "设计者分页查询自己上传的模型")
    @PreAuthorize("hasAuthority('ROLE_DESIGNER')")
    @PostMapping("/my/list")
    public Result<PageResult<ModelListVO>> getMyModels(@Valid @RequestBody ModelQueryRequest request) {
        Long designerId = getCurrentUserId();
        return Result.success(modelService.getDesignerModels(request, designerId));
    }

    /**
     * 导出模型数据
     */
    @Operation(summary = "导出模型数据", description = "管理员导出模型列表数据为Excel")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @OperationLog(type = "EXPORT", module = "模型管理", description = "导出模型数据", targetType = "MODEL")
    @PostMapping("/export")
    public void exportModels(@RequestBody ModelExportRequest request, HttpServletResponse response) {
        dataExportService.exportModels(request, response);
    }
}
