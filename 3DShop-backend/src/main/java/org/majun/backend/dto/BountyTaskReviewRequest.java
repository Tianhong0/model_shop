package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "悬赏任务审核请求")
public class BountyTaskReviewRequest {

    @NotNull(message = "任务ID不能为空")
    @Schema(description = "任务ID")
    private Long taskId;

    @NotNull(message = "审核结论不能为空")
    @Schema(description = "审核结论:1通过,2驳回")
    private Integer decision;

    @Schema(description = "审核备注")
    private String remark;
}
