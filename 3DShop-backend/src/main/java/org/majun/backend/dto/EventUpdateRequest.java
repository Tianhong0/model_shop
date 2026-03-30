package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 更新活动请求
 */
@Data
@Schema(description = "更新活动请求")
public class EventUpdateRequest {

    @NotNull(message = "活动ID不能为空")
    @Schema(description = "活动ID")
    private Long id;

    @NotBlank(message = "活动标题不能为空")
    @Schema(description = "活动标题")
    private String title;

    @NotBlank(message = "活动Banner不能为空")
    @Schema(description = "活动Banner图URL")
    private String bannerUrl;

    @NotNull(message = "活动类型不能为空")
    @Schema(description = "活动类型: 1-设计竞赛, 2-线下活动, 3-其他")
    private Integer eventType;

    @NotBlank(message = "活动介绍不能为空")
    @Schema(description = "活动介绍")
    private String description;

    @Schema(description = "参赛规则")
    private String rules;

    @Schema(description = "活动地点")
    private String location;

    @NotNull(message = "开始时间不能为空")
    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "报名开始时间")
    private LocalDateTime signupStart;

    @Schema(description = "报名截止时间")
    private LocalDateTime signupEnd;

    @Schema(description = "最大参与人数")
    private Integer maxParticipants;

    @Schema(description = "状态: 0-未开始, 1-报名中, 2-进行中, 3-评审中, 4-已结束")
    private Integer status;

    @Schema(description = "奖励列表")
    private List<EventRewardRequest> rewards;
}
