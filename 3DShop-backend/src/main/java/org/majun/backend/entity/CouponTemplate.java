package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板实体
 */
@Data
@TableName("coupon_template")
@Schema(description = "优惠券模板")
public class CouponTemplate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("name")
    @Schema(description = "优惠券名称")
    private String name;

    @TableField("type")
    @Schema(description = "类型：1-满减券，2-折扣券，3-现金券")
    private Integer type;

    @TableField("value")
    @Schema(description = "优惠值（满减金额/折扣比例/现金金额）")
    private BigDecimal value;

    @TableField("min_amount")
    @Schema(description = "最低消费门槛")
    private BigDecimal minAmount;

    @TableField("max_discount")
    @Schema(description = "最大优惠金额（折扣券用）")
    private BigDecimal maxDiscount;

    @TableField("point_cost")
    @Schema(description = "兑换所需积分")
    private Integer pointCost;

    @TableField("total_stock")
    @Schema(description = "总库存")
    private Integer totalStock;

    @TableField("remaining_stock")
    @Schema(description = "剩余库存")
    private Integer remainingStock;

    @TableField("per_user_limit")
    @Schema(description = "每人限领数量")
    private Integer perUserLimit;

    @TableField("valid_days")
    @Schema(description = "有效天数")
    private Integer validDays;

    @TableField("start_time")
    @Schema(description = "有效期开始时间")
    private LocalDateTime startTime;

    @TableField("end_time")
    @Schema(description = "有效期结束时间")
    private LocalDateTime endTime;

    @TableField("status")
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;

    @TableField("description")
    @Schema(description = "使用说明")
    private String description;

    @TableField("is_delete")
    @TableLogic
    @Schema(description = "逻辑删除")
    private Integer isDelete;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
