package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动详情VO
 */
@Data
@Schema(description = "活动详情VO")
public class EventDetailVO {

    @Schema(description = "活动ID")
    private Long id;

    @Schema(description = "活动标题")
    private String title;

    @Schema(description = "活动Banner图")
    private String bannerUrl;

    @Schema(description = "活动类型: 1-设计竞赛, 2-线下活动, 3-其他")
    private Integer eventType;

    @Schema(description = "活动类型名称")
    private String eventTypeName;

    @Schema(description = "活动介绍")
    private String description;

    @Schema(description = "参赛规则")
    private String rules;

    @Schema(description = "活动地点")
    private String location;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "报名开始时间")
    private LocalDateTime signupStart;

    @Schema(description = "报名截止时间")
    private LocalDateTime signupEnd;

    @Schema(description = "最大参与人数")
    private Integer maxParticipants;

    @Schema(description = "当前报名人数")
    private Integer currentParticipants;

    @Schema(description = "状态: 0-未开始, 1-报名中, 2-进行中, 3-评审中, 4-已结束")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "奖励列表")
    private List<EventRewardVO> rewards;

    @Schema(description = "当前用户是否已报名")
    private Boolean signedUp;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
