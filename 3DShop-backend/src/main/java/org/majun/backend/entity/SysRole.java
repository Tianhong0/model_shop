package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统角色实体类
 */
@Data
@TableName("sys_role")
@Schema(description = "系统角色")
public class SysRole implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 角色名称 (如: ROLE_ADMIN, ROLE_USER)
     */
    @TableField("role_name")
    @Schema(description = "角色名称")
    private String roleName;

    /**
     * 角色描述
     */
    @TableField("role_desc")
    @Schema(description = "角色描述")
    private String roleDesc;

    /**
     * 状态：1-启用, 0-禁用
     */
    @TableField("status")
    @Schema(description = "状态：1-启用, 0-禁用")
    private Integer status;

    /**
     * 数据范围：1-全部数据，5-仅本人数据
     */
    @TableField("data_scope")
    @Schema(description = "数据范围：1-全部数据，5-仅本人数据")
    private Integer dataScope;

    /**
     * 逻辑删除：1-已删, 0-未删
     */
    @TableField("is_delete")
    @TableLogic
    @Schema(description = "逻辑删除：1-已删, 0-未删")
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
