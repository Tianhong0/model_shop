package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 批量打印价格计算请求
 */
@Data
@Schema(description = "批量打印价格计算请求")
public class BatchPriceCalculateRequest {

    @NotNull(message = "模型ID不能为空")
    @Schema(description = "模型ID")
    private Long modelId;

    @Schema(description = "材质ID")
    private Long materialId;

    @DecimalMin(value = "0.01", message = "缩放比例必须大于0")
    @Schema(description = "缩放比例")
    private BigDecimal scale;

    @DecimalMin(value = "0", message = "填充密度必须>=0")
    @DecimalMax(value = "100", message = "填充密度必须<=100")
    @Schema(description = "填充密度")
    private BigDecimal fillPercent;

    @Min(value = 1, message = "购买数量必须>=1")
    @Schema(description = "购买数量")
    private Integer quantity = 1;
}
