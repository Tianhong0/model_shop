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
 * 活动赛事实体
 */
@Data
@TableName("sys_event")
@Schema(description = "活动赛事")
public class SysEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "活动ID")
    private Long id;

    @TableField("title")
    @Schema(description = "活动标题")
    private String title;

    @TableField("banner_url")
    @Schema(description = "活动Banner图")
    private String bannerUrl;

    @TableField("event_type")
    @Schema(description = "活动类型: 1-设计竞赛, 2-线下活动, 3-其他")
    private Integer eventType;

    @TableField("description")
    @Schema(description = "活动介绍")
    private String description;

    @TableField("rules")
    @Schema(description = "参赛规则")
    private String rules;

    @TableField("location")
    @Schema(description = "活动地点")
    private String location;

    @TableField("start_time")
    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @TableField("end_time")
    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @TableField("signup_start")
    @Schema(description = "报名开始时间")
    private LocalDateTime signupStart;

    @TableField("signup_end")
    @Schema(description = "报名截止时间")
    private LocalDateTime signupEnd;

    @TableField("max_participants")
    @Schema(description = "最大参与人数")
    private Integer maxParticipants;

    @TableField("current_participants")
    @Schema(description = "当前报名人数")
    private Integer currentParticipants;

    @TableField("status")
    @Schema(description = "状态: 0-未开始, 1-报名中, 2-进行中, 3-评审中, 4-已结束")
    private Integer status;

    @TableField("is_delete")
    @TableLogic
    @Schema(description = "逻辑删除")
    private Integer isDelete;

    @TableField("create_by")
    @Schema(description = "创建人ID")
    private Long createBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
