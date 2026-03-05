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
 * 模型分类实体类
 */
@Data
@TableName("sys_model_category")
@Schema(description = "模型分类")
public class SysModelCategory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 分类名称 (如: 动漫手办, 工业零件)
     */
    @TableField("category_name")
    @Schema(description = "分类名称")
    private String categoryName;

    /**
     * 分类编码 (如: anime, tools)
     */
    @TableField("category_code")
    @Schema(description = "分类编码")
    private String categoryCode;

    /**
     * 分类图标
     */
    @TableField("icon")
    @Schema(description = "分类图标")
    private String icon;

    /**
     * 父级分类ID (顶级为0)
     */
    @TableField("parent_id")
    @Schema(description = "父级分类ID")
    private Long parentId;

    /**
     * 父级分类名称（仅接口返回，不入库）
     */
    @TableField(exist = false)
    @Schema(description = "父级分类名称")
    private String parentName;

    /**
     * 排序编号 (越小越靠前)
     */
    @TableField("sort_no")
    @Schema(description = "排序编号")
    private Integer sortNo;

    /**
     * 状态: 1-启用, 0-禁用
     */
    @TableField("status")
    @Schema(description = "状态: 1-启用, 0-禁用")
    private Integer status;

    /**
     * 逻辑删除: 1-已删, 0-未删
     */
    @TableField("is_delete")
    @Schema(description = "逻辑删除")
    private Integer isDelete;

    /**
     * 创建时间
     */
    @TableField("create_time")
    @Schema(description = "创建时间")
    private String createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    @Schema(description = "更新时间")
    private String updateTime;
}
