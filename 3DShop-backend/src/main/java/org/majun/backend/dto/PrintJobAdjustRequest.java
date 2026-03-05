package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "打印任务调整请求")
public class PrintJobAdjustRequest {

    @NotNull
    @Schema(description = "任务ID")
    private Long jobId;

    @Schema(description = "优先级")
    private Integer priority;

    @Schema(description = "重分配打印机ID")
    private Long printerId;
}
