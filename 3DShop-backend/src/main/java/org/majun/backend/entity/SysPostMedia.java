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
@TableName("sys_post_media")
@Schema(description = "帖子媒体资源")
public class SysPostMedia implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("post_id")
    @Schema(description = "帖子ID")
    private Long postId;

    @TableField("media_url")
    @Schema(description = "媒体地址")
    private String mediaUrl;

    @TableField("media_type")
    @Schema(description = "媒体类型: 1-图片, 2-视频")
    private Integer mediaType;

    @TableField("sort_order")
    @Schema(description = "排序权重")
    private Integer sortOrder;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
