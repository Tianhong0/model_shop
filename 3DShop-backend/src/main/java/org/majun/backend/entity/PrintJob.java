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
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("print_job")
@Schema(description = "打印任务")
public class PrintJob implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_sn")
    private String orderSn;

    @TableField("model_id")
    private Long modelId;

    @TableField("model_name")
    private String modelName;

    @TableField("model_file_name")
    private String modelFileName;

    @TableField("model_file_type")
    private String modelFileType;

    @TableField("gcode_file_name")
    private String gcodeFileName;

    @TableField("layer_height")
    private BigDecimal layerHeight;

    @TableField("fill_density")
    private Integer fillDensity;

    @TableField("filament_diameter")
    private BigDecimal filamentDiameter;

    @TableField("priority")
    private Integer priority;

    @TableField("status")
    private Integer status;

    @TableField("printer_id")
    private Long printerId;

    @TableField("octoprint_job_id")
    private String octoprintJobId;

    @TableField("progress")
    private BigDecimal progress;

    @TableField("tool_temp_actual")
    private BigDecimal toolTempActual;

    @TableField("tool_temp_target")
    private BigDecimal toolTempTarget;

    @TableField("bed_temp_actual")
    private BigDecimal bedTempActual;

    @TableField("bed_temp_target")
    private BigDecimal bedTempTarget;

    @TableField("estimated_seconds_left")
    private Integer estimatedSecondsLeft;

    @TableField("error_message")
    private String errorMessage;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;

    @TableLogic
    @TableField("is_delete")
    private Integer isDelete;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
