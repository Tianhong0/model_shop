package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 邀请关系实体类
 */
@Data
@TableName("invite_relation")
@Schema(description = "邀请关系")
public class InviteRelation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("inviter_id")
    @Schema(description = "邀请人ID")
    private Long inviterId;

    @TableField("invitee_id")
    @Schema(description = "被邀请人ID")
    private Long inviteeId;

    @TableField("invite_code")
    @Schema(description = "使用的邀请码")
    private String inviteCode;

    @TableField("register_time")
    @Schema(description = "注册时间")
    private LocalDateTime registerTime;

    @TableField("first_order_time")
    @Schema(description = "首单时间")
    private LocalDateTime firstOrderTime;

    @TableField("first_order_id")
    @Schema(description = "首单订单ID")
    private Long firstOrderId;

    @TableField("total_order_count")
    @Schema(description = "累计订单数")
    private Integer totalOrderCount;

    @TableField("total_order_amount")
    @Schema(description = "累计订单金额")
    private BigDecimal totalOrderAmount;

    @TableField("status")
    @Schema(description = "状态：1-正常, 0-无效")
    private Integer status;

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
