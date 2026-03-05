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

@Data
@TableName("sys_post")
@Schema(description = "社区帖子")
public class SysPost implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("user_id")
    @Schema(description = "发布者ID")
    private Long userId;

    @TableField("category_id")
    @Schema(description = "分类ID")
    private Long categoryId;

    @TableField("title")
    @Schema(description = "帖子标题")
    private String title;

    @TableField("content")
    @Schema(description = "帖子正文")
    private String content;

    @TableField("view_count")
    @Schema(description = "浏览量")
    private Integer viewCount;

    @TableField("like_count")
    @Schema(description = "点赞数")
    private Integer likeCount;

    @TableField("collect_count")
    @Schema(description = "收藏数")
    private Integer collectCount;

    @TableField("is_top")
    @Schema(description = "是否置顶: 1-是, 0-否")
    private Integer isTop;

    @TableField("status")
    @Schema(description = "状态: 0-草稿, 1-发布, 2-下架")
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
