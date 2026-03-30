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
import java.time.LocalDateTime;

/**
 * 用户活动参与实体
 */
@Data
@TableName("sys_event_participation")
@Schema(description = "用户活动参与")
public class SysEventParticipation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "参与记录ID")
    private Long id;

    @TableField("event_id")
    @Schema(description = "活动ID")
    private Long eventId;

    @TableField("user_id")
    @Schema(description = "用户ID")
    private Long userId;

    @TableField("signup_time")
    @Schema(description = "报名时间")
    private LocalDateTime signupTime;

    @TableField("status")
    @Schema(description = "状态: 1-已报名, 2-已签到, 3-已提交作品, 4-已获奖, 5-已取消")
    private Integer status;

    @TableField("checkin_time")
    @Schema(description = "签到时间")
    private LocalDateTime checkinTime;

    @TableField("award_rank")
    @Schema(description = "获奖奖项")
    private String awardRank;

    @TableField("result")
    @Schema(description = "参与结果")
    private String result;

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
