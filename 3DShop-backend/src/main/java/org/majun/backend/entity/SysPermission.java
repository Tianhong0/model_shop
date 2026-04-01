package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统权限实体类
 */
@Data
@TableName("sys_permission")
@Schema(description = "系统权限")
public class SysPermission implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 权限标识
     */
    @TableField("permission_code")
    @Schema(description = "权限标识，如：user:list, user:create")
    private String permissionCode;

    /**
     * 权限名称
     */
    @TableField("permission_name")
    @Schema(description = "权限名称")
    private String permissionName;

    /**
     * 权限类型：MENU-菜单, BUTTON-按钮, API-接口
     */
    @TableField("permission_type")
    @Schema(description = "权限类型：MENU/BUTTON/API")
    private String permissionType;

    /**
     * 父级ID
     */
    @TableField("parent_id")
    @Schema(description = "父级ID")
    private Long parentId;

    /**
     * 菜单路径
     */
    @TableField("menu_path")
    @Schema(description = "菜单路径")
    private String menuPath;

    /**
     * 图标
     */
    @TableField("icon")
    @Schema(description = "图标")
    private String icon;

    /**
     * 排序
     */
    @TableField("sort_order")
    @Schema(description = "排序")
    private Integer sortOrder;

    /**
     * 状态：1-启用, 0-禁用
     */
    @TableField("status")
    @Schema(description = "状态：1-启用, 0-禁用")
    private Integer status;

    /**
     * 逻辑删除
     */
    @TableField("is_delete")
    @TableLogic
    @Schema(description = "逻辑删除")
    private Integer isDelete;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
