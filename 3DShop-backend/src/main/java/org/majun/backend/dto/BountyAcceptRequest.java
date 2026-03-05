package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "验收请求")
public class BountyAcceptRequest {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotNull(message = "交付ID不能为空")
    private Long deliveryId;

    @NotNull(message = "决策不能为空")
    @Schema(description = "验收决策:1通过,2驳回")
    private Integer decision;

    @Schema(description = "备注")
    private String remark;
}
