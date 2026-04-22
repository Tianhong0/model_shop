package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 推广配置实体类
 */
@Data
@TableName("promotion_config")
@Schema(description = "推广配置")
public class PromotionConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("config_key")
    @Schema(description = "配置键")
    private String configKey;

    @TableField("config_value")
    @Schema(description = "配置值")
    private String configValue;

    @TableField("config_desc")
    @Schema(description = "配置描述")
    private String configDesc;

    @TableField("status")
    @Schema(description = "状态：1-启用, 0-禁用")
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
