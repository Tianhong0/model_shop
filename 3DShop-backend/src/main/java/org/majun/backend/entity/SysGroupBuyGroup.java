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
 * 拼团组实体
 */
@Data
@TableName("sys_group_buy_group")
@Schema(description = "拼团组")
public class SysGroupBuyGroup implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "拼团组ID")
    private Long id;

    @TableField("activity_id")
    @Schema(description = "活动ID")
    private Long activityId;

    @TableField("leader_user_id")
    @Schema(description = "团长用户ID")
    private Long leaderUserId;

    @TableField("group_sn")
    @Schema(description = "拼团编号")
    private String groupSn;

    @TableField("current_people")
    @Schema(description = "当前人数")
    private Integer currentPeople;

    @TableField("target_people")
    @Schema(description = "目标人数")
    private Integer targetPeople;

    @TableField("status")
    @Schema(description = "状态：0-拼团中，1-拼团成功，2-拼团失败，3-已取消")
    private Integer status;

    @TableField("total_amount")
    @Schema(description = "拼团总金额")
    private BigDecimal totalAmount;

    @TableField("expire_time")
    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @TableField("success_time")
    @Schema(description = "成功时间")
    private LocalDateTime successTime;

    @TableField("fail_time")
    @Schema(description = "失败时间")
    private LocalDateTime failTime;

    @TableField("fail_reason")
    @Schema(description = "失败原因")
    private String failReason;

    @TableField("share_code")
    @Schema(description = "分享码")
    private String shareCode;

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
