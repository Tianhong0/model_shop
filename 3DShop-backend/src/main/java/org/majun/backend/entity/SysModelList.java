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
@TableName("sys_model_list")
@Schema(description = "模型清单")
public class SysModelList implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("user_id")
    @Schema(description = "创建者ID")
    private Long userId;

    @TableField("title")
    @Schema(description = "清单标题")
    private String title;

    @TableField("description")
    @Schema(description = "清单描述")
    private String description;

    @TableField("cover_image")
    @Schema(description = "封面图URL")
    private String coverImage;

    @TableField("model_count")
    @Schema(description = "包含模型数量")
    private Integer modelCount;

    @TableField("view_count")
    @Schema(description = "浏览量")
    private Integer viewCount;

    @TableField("like_count")
    @Schema(description = "点赞数")
    private Integer likeCount;

    @TableField("collect_count")
    @Schema(description = "收藏数")
    private Integer collectCount;

    @TableField("status")
    @Schema(description = "状态: 0-草稿, 1-已发布, 2-已下架")
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
