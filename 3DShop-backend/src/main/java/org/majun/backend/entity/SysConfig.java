package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统配置实体类
 */
@Data
@TableName("sys_config")
@Schema(description = "系统配置")
public class SysConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 配置键
     */
    @TableField("config_key")
    @Schema(description = "配置键")
    private String configKey;

    /**
     * 配置值
     */
    @TableField("config_value")
    @Schema(description = "配置值")
    private String configValue;

    /**
     * 配置类型
     */
    @TableField("config_type")
    @Schema(description = "配置类型：STRING/NUMBER/BOOLEAN/JSON")
    private String configType;

    /**
     * 配置分组
     */
    @TableField("config_group")
    @Schema(description = "配置分组：SYSTEM/PAYMENT/STORAGE等")
    private String configGroup;

    /**
     * 配置说明
     */
    @TableField("description")
    @Schema(description = "配置说明")
    private String description;

    /**
     * 是否公开：1-公开, 0-需权限
     */
    @TableField("is_public")
    @Schema(description = "是否公开")
    private Integer isPublic;

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
