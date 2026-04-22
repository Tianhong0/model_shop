package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "推广中心首页数据")
public class PromotionCenterVO {

    @Schema(description = "邀请码信息")
    private InviteCodeVO inviteCode;

    @Schema(description = "今日邀请人数")
    private Integer todayInvited;

    @Schema(description = "今日获得积分")
    private Integer todayPoints;

    @Schema(description = "总邀请人数")
    private Integer totalInvited;

    @Schema(description = "总获得积分")
    private Integer totalPoints;

    @Schema(description = "推广订单数")
    private Integer totalOrders;

    @Schema(description = "推广订单金额")
    private BigDecimal totalOrderAmount;
}
