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
 * 活动作品提交实体
 */
@Data
@TableName("sys_event_submission")
@Schema(description = "活动作品提交")
public class SysEventSubmission implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "作品ID")
    private Long id;

    @TableField("event_id")
    @Schema(description = "活动ID")
    private Long eventId;

    @TableField("participation_id")
    @Schema(description = "参与记录ID")
    private Long participationId;

    @TableField("user_id")
    @Schema(description = "用户ID")
    private Long userId;

    @TableField("title")
    @Schema(description = "作品标题")
    private String title;

    @TableField("description")
    @Schema(description = "作品描述")
    private String description;

    @TableField("file_urls")
    @Schema(description = "作品文件URL(JSON数组)")
    private String fileUrls;

    @TableField("image_urls")
    @Schema(description = "作品图片URL(JSON数组)")
    private String imageUrls;

    @TableField("status")
    @Schema(description = "状态: 1-待审核, 2-已通过, 3-已拒绝")
    private Integer status;

    @TableField("score")
    @Schema(description = "评分")
    private BigDecimal score;

    @TableField("like_count")
    @Schema(description = "点赞数")
    private Integer likeCount;

    @TableField("comment_count")
    @Schema(description = "评论数")
    private Integer commentCount;

    @TableField("review_remark")
    @Schema(description = "审核备注")
    private String reviewRemark;

    @TableField("reviewer_id")
    @Schema(description = "审核人ID")
    private Long reviewerId;

    @TableField("review_time")
    @Schema(description = "审核时间")
    private LocalDateTime reviewTime;

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
