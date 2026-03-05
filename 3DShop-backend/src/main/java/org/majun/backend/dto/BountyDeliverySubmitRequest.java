package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "交付提交请求")
public class BountyDeliverySubmitRequest {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotBlank(message = "交付说明不能为空")
    private String description;

    @Schema(description = "是否最终交付:1是,0否")
    private Integer isFinal = 1;
}
