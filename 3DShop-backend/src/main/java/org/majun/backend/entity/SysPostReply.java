package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_post_reply")
@Schema(description = "帖子回复")
public class SysPostReply implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("post_id")
    @Schema(description = "帖子ID")
    private Long postId;

    @TableField("user_id")
    @Schema(description = "回复用户ID")
    private Long userId;

    @TableField("parent_id")
    @Schema(description = "父回复ID")
    private Long parentId;

    @TableField("content")
    @Schema(description = "回复内容")
    private String content;

    @TableField("is_adopted")
    @Schema(description = "是否采纳: 1-是, 0-否")
    private Integer isAdopted;

    @TableField("is_excellent")
    @Schema(description = "是否优质回答: 1-是, 0-否")
    private Integer isExcellent;

    @TableField("like_count")
    @Schema(description = "点赞数")
    private Integer likeCount;

    @TableField("status")
    @Schema(description = "状态: 1-正常, 0-屏蔽")
    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
