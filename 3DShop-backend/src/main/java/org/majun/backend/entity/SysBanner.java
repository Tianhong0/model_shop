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
 * 轮播图实体
 */
@Data
@TableName("sys_banner")
@Schema(description = "主页轮播图")
public class SysBanner implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("title")
    @Schema(description = "轮播图标题")
    private String title;

    @TableField("image_url")
    @Schema(description = "图片地址")
    private String imageUrl;

    @TableField("link_type")
    @Schema(description = "跳转类型")
    private Integer linkType;

    @TableField("link_value")
    @Schema(description = "跳转值")
    private String linkValue;

    @TableField("sort_no")
    @Schema(description = "排序")
    private Integer sortNo;

    @TableField("status")
    @Schema(description = "状态: 1-启用, 0-禁用")
    private Integer status;

    @TableField("start_time")
    @Schema(description = "生效开始时间")
    private LocalDateTime startTime;

    @TableField("end_time")
    @Schema(description = "生效结束时间")
    private LocalDateTime endTime;

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
