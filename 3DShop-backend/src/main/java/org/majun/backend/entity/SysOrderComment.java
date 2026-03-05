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
@TableName("sys_order_comment")
@Schema(description = "Order comment")
public class SysOrderComment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "Comment ID")
    private Long id;

    @TableField("order_id")
    @Schema(description = "Order ID")
    private Long orderId;

    @TableField("user_id")
    @Schema(description = "User ID")
    private Long userId;

    @TableField("model_id")
    @Schema(description = "Model ID")
    private Long modelId;

    @TableField("model_score")
    @Schema(description = "Model score")
    private Integer modelScore;

    @TableField("print_score")
    @Schema(description = "Print score")
    private Integer printScore;

    @TableField("service_score")
    @Schema(description = "Service score")
    private Integer serviceScore;

    @TableField("comment_text")
    @Schema(description = "Comment text")
    private String commentText;

    @TableField("comment_images")
    @Schema(description = "Comment images, comma separated")
    private String commentImages;

    @TableField("is_anonymous")
    @Schema(description = "Is anonymous")
    private Integer isAnonymous;

    @TableField("reply_content")
    @Schema(description = "Reply content")
    private String replyContent;

    @TableField("reply_time")
    @Schema(description = "Reply time")
    private LocalDateTime replyTime;

    @TableField("like_count")
    @Schema(description = "Like count")
    private Integer likeCount;

    @TableField("reply_count")
    @Schema(description = "Reply count")
    private Integer replyCount;

    @TableField("status")
    @Schema(description = "Status: 1 normal, 0 hidden")
    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "Create time")
    private LocalDateTime createTime;
}
