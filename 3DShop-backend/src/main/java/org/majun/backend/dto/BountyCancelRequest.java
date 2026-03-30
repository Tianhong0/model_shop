package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "取消悬赏请求")
public class BountyCancelRequest {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @Schema(description = "取消原因")
    private String reason;
}
