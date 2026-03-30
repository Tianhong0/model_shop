package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "管理员审核取消悬赏请求")
public class BountyCancelReviewRequest {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotNull(message = "决策不能为空")
    @Schema(description = "审核决策:1同意取消,2拒绝取消")
    private Integer decision;

    @Schema(description = "备注")
    private String remark;
}
