package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 参与状态更新请求
 */
@Data
@Schema(description = "参与状态更新请求")
public class ParticipationStatusUpdateRequest {

    @NotNull(message = "参与记录ID不能为空")
    @Schema(description = "参与记录ID")
    private Long id;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态: 1-已报名, 2-已签到, 3-已提交作品, 4-已获奖, 5-已取消")
    private Integer status;

    @Schema(description = "获奖奖项(状态为已获奖时填写)")
    private String awardRank;

    @Schema(description = "参与结果")
    private String result;
}
