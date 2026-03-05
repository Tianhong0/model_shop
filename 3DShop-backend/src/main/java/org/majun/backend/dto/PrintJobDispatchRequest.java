package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "手动排产请求")
public class PrintJobDispatchRequest {

    @NotNull
    @Schema(description = "订单ID")
    private Long orderId;

    @NotNull
    @Schema(description = "打印机ID")
    private Long printerId;

    @NotNull
    @DecimalMin(value = "0.05", message = "layerHeight too small")
    @Schema(description = "层高")
    private Double layerHeight;

    @NotNull
    @Min(value = 0)
    @Max(value = 100)
    @Schema(description = "填充密度")
    private Integer fillDensity;

    @NotNull
    @DecimalMin(value = "1.0", message = "filamentDiameter too small")
    @Schema(description = "丝径")
    private Double filamentDiameter;

    @Schema(description = "优先级(越大越优先)")
    private Integer priority = 1;
}
