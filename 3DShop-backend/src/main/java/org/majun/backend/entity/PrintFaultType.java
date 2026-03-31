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

/**
 * 打印故障类型配置实体
 */
@Data
@TableName("print_fault_type")
@Schema(description = "打印故障类型配置")
public class PrintFaultType implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("fault_code")
    @Schema(description = "故障代码")
    private String faultCode;

    @TableField("fault_category")
    @Schema(description = "故障分类（MODEL/PARAM/MATERIAL/DEVICE/UNKNOWN）")
    private String faultCategory;

    @TableField("fault_name")
    @Schema(description = "故障名称")
    private String faultName;

    @TableField("description")
    @Schema(description = "故障描述")
    private String description;

    @TableField("suggestion")
    @Schema(description = "处理建议（JSON数组格式）")
    private String suggestion;

    @TableField("error_keywords")
    @Schema(description = "错误关键词匹配（JSON数组格式）")
    private String errorKeywords;

    @TableField("priority")
    @Schema(description = "匹配优先级（数值越大优先级越高）")
    private Integer priority;

    @TableField("is_active")
    @Schema(description = "是否启用")
    private Integer isActive;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
