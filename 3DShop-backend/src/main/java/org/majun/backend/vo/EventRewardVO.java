package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 活动奖励VO
 */
@Data
@Schema(description = "活动奖励VO")
public class EventRewardVO {

    @Schema(description = "奖项名称")
    private String rankName;

    @Schema(description = "获奖人数")
    private Integer winnerCount;

    @Schema(description = "奖品内容")
    private String prizeContent;
}
