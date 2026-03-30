package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拼团参与实体
 */
@Data
@TableName("sys_group_buy_participant")
@Schema(description = "拼团参与")
public class SysGroupBuyParticipant implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "参与ID")
    private Long id;

    @TableField("group_id")
    @Schema(description = "拼团ID")
    private Long groupId;

    @TableField("activity_id")
    @Schema(description = "活动ID")
    private Long activityId;

    @TableField("user_id")
    @Schema(description = "用户ID")
    private Long userId;

    @TableField("order_id")
    @Schema(description = "关联订单ID")
    private Long orderId;

    @TableField("order_sn")
    @Schema(description = "订单号")
    private String orderSn;

    @TableField("is_leader")
    @Schema(description = "是否团长")
    private Integer isLeader;

    @TableField("quantity")
    @Schema(description = "购买数量")
    private Integer quantity;

    @TableField("unit_price")
    @Schema(description = "单价")
    private BigDecimal unitPrice;

    @TableField("total_amount")
    @Schema(description = "总金额")
    private BigDecimal totalAmount;

    @TableField("material_id")
    @Schema(description = "材质ID")
    private Long materialId;

    @TableField("color")
    @Schema(description = "颜色")
    private String color;

    @TableField("scale")
    @Schema(description = "缩放比例")
    private BigDecimal scale;

    @TableField("fill_percent")
    @Schema(description = "填充密度")
    private BigDecimal fillPercent;

    @TableField("custom_params")
    @Schema(description = "其他定制参数")
    private String customParams;

    @TableField("status")
    @Schema(description = "状态：0-待支付，1-已支付，2-已取消，3-已退款")
    private Integer status;

    @TableField("pay_time")
    @Schema(description = "支付时间")
    private LocalDateTime payTime;

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
