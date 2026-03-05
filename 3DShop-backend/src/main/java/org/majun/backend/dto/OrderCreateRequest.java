package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Order create request.
 */
@Data
@Schema(description = "Order create request")
public class OrderCreateRequest {

    @NotNull(message = "Model ID is required")
    @Schema(description = "Model ID")
    private Long modelId;

    @Schema(description = "Material ID")
    private Long materialId;

    @DecimalMin(value = "0.01", message = "Scale must be greater than 0")
    @Schema(description = "Scale ratio")
    private BigDecimal scale;

    @DecimalMin(value = "0", message = "Fill percent must be >= 0")
    @DecimalMax(value = "100", message = "Fill percent must be <= 100")
    @Schema(description = "Fill percent (0-100)")
    private BigDecimal fillPercent;

    @Schema(description = "Color")
    private String color;

    @Schema(description = "Note")
    private String note;

    @Schema(description = "Custom params JSON from model customization page")
    private String customParams;

    @Schema(description = "使用积分数量")
    private Integer usePoints;
}
