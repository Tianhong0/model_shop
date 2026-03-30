package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户活动参与VO
 */
@Data
@Schema(description = "用户活动参与VO")
public class EventParticipationVO {

    @Schema(description = "参与记录ID")
    private Long id;

    @Schema(description = "活动ID")
    private Long eventId;

    @Schema(description = "活动标题")
    private String eventTitle;

    @Schema(description = "活动Banner图")
    private String eventBanner;

    @Schema(description = "活动类型: 1-设计竞赛, 2-线下活动, 3-其他")
    private Integer eventType;

    @Schema(description = "活动类型名称")
    private String eventTypeName;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户昵称")
    private String userName;

    @Schema(description = "用户头像")
    private String userAvatar;

    @Schema(description = "报名时间")
    private LocalDateTime signupTime;

    @Schema(description = "签到时间")
    private LocalDateTime checkinTime;

    @Schema(description = "状态: 1-已报名, 2-已签到, 3-已提交作品, 4-已获奖, 5-已取消")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "获奖奖项")
    private String awardRank;

    @Schema(description = "参与结果")
    private String result;

    @Schema(description = "活动状态: 0-未开始, 1-报名中, 2-进行中, 3-评审中, 4-已结束")
    private Integer eventStatus;

    @Schema(description = "活动状态名称")
    private String eventStatusName;
}
