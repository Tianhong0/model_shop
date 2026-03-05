package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新模型请求DTO
 */
@Data
@Schema(description = "更新模型请求")
public class ModelUpdateRequest {

    /**
     * 模型ID
     */
    @NotNull(message = "模型ID不能为空")
    @Schema(description = "模型ID")
    private Long id;

    /**
     * 模型名称
     */
    @Schema(description = "模型名称")
    private String modelName;

    /**
     * 分类ID
     */
    @Schema(description = "分类ID")
    private Long categoryId;

    /**
     * 基础价格
     */
    @Schema(description = "基础价格")
    private BigDecimal basePrice;

    /**
     * 原始体积 (mm³)
     */
    @Schema(description = "原始体积(mm³)")
    private BigDecimal baseVolume;

    /**
     * 原始三维尺寸
     */
    @Schema(description = "原始三维尺寸(L*W*H)")
    private String baseSize;

    /**
     * 模型文件路径
     */
    @Schema(description = "模型文件路径")
    private String filePath;

    /**
     * 模型描述
     */
    @Schema(description = "模型描述")
    private String description;

    /**
     * 主图URL
     */
    @Schema(description = "主图URL")
    private String mainImageUrl;

    /**
     * 授权说明
     */
    @Schema(description = "授权说明")
    private String licenseType;

    /**
     * 上架状态: 0-审核中, 1-上架, 2-下架
     */
    @Schema(description = "上架状态: 0-审核中, 1-上架, 2-下架")
    private Integer status;
}
