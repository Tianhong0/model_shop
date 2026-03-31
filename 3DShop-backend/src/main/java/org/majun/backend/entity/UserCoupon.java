package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户优惠券实体
 */
@Data
@TableName("user_coupon")
@Schema(description = "用户优惠券")
public class UserCoupon implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("user_id")
    @Schema(description = "用户ID")
    private Long userId;

    @TableField("template_id")
    @Schema(description = "优惠券模板ID")
    private Long templateId;

    @TableField("coupon_no")
    @Schema(description = "优惠券编号")
    private String couponNo;

    @TableField("type")
    @Schema(description = "类型：1-满减券，2-折扣券，3-现金券")
    private Integer type;

    @TableField("value")
    @Schema(description = "优惠值")
    private BigDecimal value;

    @TableField("min_amount")
    @Schema(description = "最低消费门槛")
    private BigDecimal minAmount;

    @TableField("max_discount")
    @Schema(description = "最大优惠金额")
    private BigDecimal maxDiscount;

    @TableField("status")
    @Schema(description = "状态：0-未使用，1-已使用，2-已过期")
    private Integer status;

    @TableField("order_id")
    @Schema(description = "使用的订单ID")
    private Long orderId;

    @TableField("used_time")
    @Schema(description = "使用时间")
    private LocalDateTime usedTime;

    @TableField("start_time")
    @Schema(description = "有效期开始时间")
    private LocalDateTime startTime;

    @TableField("end_time")
    @Schema(description = "有效期结束时间")
    private LocalDateTime endTime;

    @TableField("point_cost")
    @Schema(description = "兑换消耗的积分")
    private Integer pointCost;

    @TableField("is_delete")
    @TableLogic
    @Schema(description = "逻辑删除")
    private Integer isDelete;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "领取时间")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
