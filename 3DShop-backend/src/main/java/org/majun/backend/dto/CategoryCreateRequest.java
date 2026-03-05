package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建分类请求DTO
 */
@Data
@Schema(description = "创建分类请求")
public class CategoryCreateRequest {

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    @Schema(description = "分类名称")
    private String categoryName;

    /**
     * 分类编码
     */
    @NotBlank(message = "分类编码不能为空")
    @Schema(description = "分类编码")
    private String categoryCode;

    /**
     * 分类图标
     */
    @Schema(description = "分类图标")
    private String icon;

    /**
     * 父级分类ID (顶级为0或null)
     */
    @Schema(description = "父级分类ID(顶级为0或null)")
    private Long parentId;

    /**
     * 排序编号 (越小越靠前)
     */
    @Schema(description = "排序编号(越小越靠前)")
    private Integer sortNo;

    /**
     * 状态: 1-启用, 0-禁用
     */
    @Schema(description = "状态: 1-启用, 0-禁用")
    private Integer status;
}
