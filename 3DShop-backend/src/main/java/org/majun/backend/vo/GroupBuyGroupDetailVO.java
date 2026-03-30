package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 拼团详情VO
 */
@Data
@Schema(description = "拼团详情VO")
public class GroupBuyGroupDetailVO {

    @Schema(description = "拼团组ID")
    private Long id;

    @Schema(description = "活动ID")
    private Long activityId;

    @Schema(description = "活动信息")
    private GroupBuyActivityVO activity;

    @Schema(description = "拼团编号")
    private String groupSn;

    @Schema(description = "分享码")
    private String shareCode;

    @Schema(description = "当前人数")
    private Integer currentPeople;

    @Schema(description = "目标人数")
    private Integer targetPeople;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "剩余时间（秒）")
    private Long remainingSeconds;

    @Schema(description = "团长信息")
    private GroupBuyParticipantVO leader;

    @Schema(description = "参与成员列表")
    private List<GroupBuyParticipantVO> participants;

    @Schema(description = "当前用户是否已参与")
    private Boolean hasJoined;

    @Schema(description = "当前用户参与信息")
    private GroupBuyParticipantVO myParticipant;
}
