package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新分类请求DTO
 */
@Data
@Schema(description = "更新分类请求")
public class CategoryUpdateRequest {

    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    @Schema(description = "分类ID")
    private Long id;

    /**
     * 分类名称
     */
    @Schema(description = "分类名称")
    private String categoryName;

    /**
     * 分类编码
     */
    @Schema(description = "分类编码")
    private String categoryCode;

    /**
     * 分类图标
     */
    @Schema(description = "分类图标")
    private String icon;

    /**
     * 父级分类ID
     */
    @Schema(description = "父级分类ID")
    private Long parentId;

    /**
     * 排序编号
     */
    @Schema(description = "排序编号")
    private Integer sortNo;

    /**
     * 状态: 1-启用, 0-禁用
     */
    @Schema(description = "状态: 1-启用, 0-禁用")
    private Integer status;
}
