package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "打印任务查询")
public class PrintJobQueryRequest {

    @Min(value = 1, message = "Page number must be >= 1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "Page size must be >= 1")
    private Integer pageSize = 10;

    @Schema(description = "任务状态")
    private Integer status;

    @Schema(description = "打印机ID")
    private Long printerId;

    @Schema(description = "订单号")
    private String orderSn;
}
