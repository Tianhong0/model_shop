package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拼团组VO
 */
@Data
@Schema(description = "拼团组VO")
public class GroupBuyGroupVO {

    @Schema(description = "拼团组ID")
    private Long id;

    @Schema(description = "活动ID")
    private Long activityId;

    @Schema(description = "活动名称")
    private String activityName;

    @Schema(description = "拼团编号")
    private String groupSn;

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

    @Schema(description = "团长用户ID")
    private Long leaderUserId;

    @Schema(description = "团长昵称")
    private String leaderNickname;

    @Schema(description = "团长头像")
    private String leaderAvatar;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "模型图片")
    private String modelImage;

    @Schema(description = "拼团价")
    private BigDecimal groupPrice;
}
