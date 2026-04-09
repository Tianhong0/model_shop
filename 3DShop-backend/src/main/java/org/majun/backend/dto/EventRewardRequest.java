package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 活动奖励请求
 */
@Data
@Schema(description = "活动奖励请求")
public class EventRewardRequest {

    @NotBlank(message = "奖项名称不能为空")
    @Schema(description = "奖项名称")
    private String rankName;

    @Schema(description = "奖项排序")
    private Integer rankOrder;

    @Schema(description = "获奖人数")
    private Integer winnerCount;

    @NotBlank(message = "奖品内容不能为空")
    @Schema(description = "奖品内容")
    private String prizeContent;

    @Schema(description = "奖励积分数量")
    private Integer points;
}
