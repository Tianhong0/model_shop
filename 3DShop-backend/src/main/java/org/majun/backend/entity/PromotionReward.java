package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 推广奖励记录实体类
 */
@Data
@TableName("promotion_reward")
@Schema(description = "推广奖励记录")
public class PromotionReward implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("user_id")
    @Schema(description = "获得奖励的用户ID")
    private Long userId;

    @TableField("invite_relation_id")
    @Schema(description = "关联邀请关系ID")
    private Long inviteRelationId;

    @TableField("reward_type")
    @Schema(description = "奖励类型")
    private String rewardType;

    @TableField("reward_points")
    @Schema(description = "奖励积分")
    private Integer rewardPoints;

    @TableField("ref_type")
    @Schema(description = "关联类型")
    private String refType;

    @TableField("ref_id")
    @Schema(description = "关联ID")
    private Long refId;

    @TableField("ref_amount")
    @Schema(description = "关联金额")
    private BigDecimal refAmount;

    @TableField("status")
    @Schema(description = "状态：1-已发放, 0-待发放, 2-已取消")
    private Integer status;

    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

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
