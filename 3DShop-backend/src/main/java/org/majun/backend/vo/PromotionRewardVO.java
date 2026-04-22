package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "推广奖励记录")
public class PromotionRewardVO {

    @Schema(description = "记录ID")
    private String id;

    @Schema(description = "奖励类型")
    private String rewardType;

    @Schema(description = "奖励类型描述")
    private String rewardTypeDesc;

    @Schema(description = "奖励积分")
    private Integer rewardPoints;

    @Schema(description = "关联金额")
    private BigDecimal refAmount;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
