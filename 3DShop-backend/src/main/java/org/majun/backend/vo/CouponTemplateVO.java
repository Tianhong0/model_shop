package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板VO
 */
@Data
@Schema(description = "优惠券模板VO")
public class CouponTemplateVO {

    @Schema(description = "模板ID")
    private Long id;

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

    @Schema(description = "兑换所需积分")
    private Integer pointCost;

    @Schema(description = "剩余库存")
    private Integer remainingStock;

    @Schema(description = "总库存")
    private Integer totalStock;

    @Schema(description = "每人限领数量")
    private Integer perUserLimit;

    @Schema(description = "有效天数")
    private Integer validDays;

    @Schema(description = "有效期开始时间")
    private LocalDateTime startTime;

    @Schema(description = "有效期结束时间")
    private LocalDateTime endTime;

    @Schema(description = "使用说明")
    private String description;

    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "当前用户已领取数量")
    private Integer userReceivedCount;
}
