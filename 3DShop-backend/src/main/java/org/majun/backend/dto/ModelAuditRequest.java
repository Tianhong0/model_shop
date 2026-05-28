package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 模型审核请求DTO
 */
@Data
@Schema(description = "模型审核请求")
public class ModelAuditRequest {

    @NotNull(message = "模型ID不能为空")
    @Schema(description = "模型ID")
    private Long modelId;

    @NotNull(message = "审核动作不能为空")
    @Schema(description = "审核动作: 1-通过, 2-驳回")
    private Integer action;

    @Schema(description = "设计师分润比例(百分比，通过时必填)")
    private Integer profitShareRatio;

    @Schema(description = "审核备注/驳回原因")
    private String note;

    @Schema(description = "管理员调整的基础价格")
    private BigDecimal basePrice;

    @Schema(description = "管理员调整的原始体积")
    private BigDecimal baseVolume;

    @Schema(description = "管理员调整的三维尺寸")
    private String baseSize;
}
