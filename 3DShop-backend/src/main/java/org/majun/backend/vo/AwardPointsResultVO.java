package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 颁奖结果
 */
@Data
@Builder
@Schema(description = "颁奖结果")
public class AwardPointsResultVO {

    @Schema(description = "成功发放人数")
    private Integer successCount;

    @Schema(description = "失败人数")
    private Integer failCount;

    @Schema(description = "已发放跳过人数")
    private Integer skippedCount;

    @Schema(description = "总发放积分")
    private Integer totalPoints;
}
