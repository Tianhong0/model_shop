package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户优惠券VO
 */
@Data
@Schema(description = "用户优惠券VO")
public class UserCouponVO {

    @Schema(description = "优惠券ID")
    private Long id;

    @Schema(description = "优惠券编号")
    private String couponNo;

    @Schema(description = "优惠券名称")
    private String name;

    @Schema(description = "类型：1-满减券，2-折扣券，3-现金券")
    private Integer type;

    @Schema(description = "类型描述")
    private String typeDesc;

    @Schema(description = "优惠值")
    private BigDecimal value;

    @Schema(description = "最低消费门槛")
    private BigDecimal minAmount;

    @Schema(description = "最大优惠金额")
    private BigDecimal maxDiscount;

    @Schema(description = "状态：0-未使用，1-已使用，2-已过期")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "有效期开始时间")
    private LocalDateTime startTime;

    @Schema(description = "有效期结束时间")
    private LocalDateTime endTime;

    @Schema(description = "使用的订单ID")
    private Long orderId;

    @Schema(description = "使用时间")
    private LocalDateTime usedTime;

    @Schema(description = "兑换消耗的积分")
    private Integer pointCost;

    @Schema(description = "领取时间")
    private LocalDateTime createTime;

    @Schema(description = "是否可用")
    private Boolean available;
}
