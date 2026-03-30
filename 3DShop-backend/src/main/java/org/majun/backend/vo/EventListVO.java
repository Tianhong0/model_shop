package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动列表VO（移动端）
 */
@Data
@Schema(description = "活动列表VO(移动端)")
public class EventListVO {

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

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "活动地点")
    private String location;

    @Schema(description = "状态: 0-未开始, 1-报名中, 2-进行中, 3-评审中, 4-已结束")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "当前报名人数")
    private Integer currentParticipants;
}
