package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "选标请求")
public class BountyPickBidRequest {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotNull(message = "竞标ID不能为空")
    private Long bidId;

    @Schema(description = "选标备注")
    private String pickReason;
}
