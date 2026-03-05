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
@TableName("sys_order_comment_reply")
@Schema(description = "订单评价追评")
public class SysOrderCommentReply implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "追评ID")
    private Long id;

    @TableField("comment_id")
    @Schema(description = "评价ID")
    private Long commentId;

    @TableField("user_id")
    @Schema(description = "用户ID")
    private Long userId;

    @TableField("content")
    @Schema(description = "追评内容")
    private String content;

    @TableField("status")
    @Schema(description = "状态: 1-正常, 0-屏蔽")
    private Integer status;

    @TableField("like_count")
    @Schema(description = "点赞数")
    private Integer likeCount;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
