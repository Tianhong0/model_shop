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
 * 打印故障诊断记录实体
 */
@Data
@TableName("print_fault_diagnosis")
@Schema(description = "打印故障诊断记录")
public class PrintFaultDiagnosis implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("job_id")
    @Schema(description = "打印任务ID")
    private Long jobId;

    @TableField("order_id")
    @Schema(description = "订单ID")
    private Long orderId;

    @TableField("user_id")
    @Schema(description = "用户ID")
    private Long userId;

    @TableField("fault_type_id")
    @Schema(description = "故障类型ID")
    private Long faultTypeId;

    @TableField("fault_code")
    @Schema(description = "故障代码")
    private String faultCode;

    @TableField("fault_category")
    @Schema(description = "故障分类")
    private String faultCategory;

    @TableField("fault_name")
    @Schema(description = "故障名称")
    private String faultName;

    @TableField("error_message")
    @Schema(description = "原始错误信息")
    private String errorMessage;

    @TableField("analysis_result")
    @Schema(description = "诊断分析结果（JSON）")
    private String analysisResult;

    @TableField("status")
    @Schema(description = "处理状态（0-未处理 1-已重试 2-已联系客服 3-已解决）")
    private Integer status;

    @TableField("retry_count")
    @Schema(description = "重试次数")
    private Integer retryCount;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
