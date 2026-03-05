package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 模型图片实体类
 */
@Data
@TableName("sys_model_image")
@Schema(description = "模型多媒体资源关联")
public class SysModelImage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 关联模型ID
     */
    @TableField("model_id")
    @Schema(description = "关联模型ID")
    private Long modelId;

    /**
     * 图片存储路径
     */
    @TableField("image_url")
    @Schema(description = "图片存储路径")
    private String imageUrl;

    /**
     * 是否为主图: 1-是, 0-否
     */
    @TableField("is_main")
    @Schema(description = "是否为主图")
    private Integer isMain;

    /**
     * 图片类型: 1-3D渲染图, 2-实物效果图, 3-参数图
     */
    @TableField("img_type")
    @Schema(description = "图片类型: 1-3D渲染图, 2-实物效果图, 3-参数图")
    private Integer imgType;

    /**
     * 排序权重 (数字越小越靠前)
     */
    @TableField("sort_order")
    @Schema(description = "排序权重")
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @TableField("create_time")
    @Schema(description = "创建时间")
    private String createTime;
}
