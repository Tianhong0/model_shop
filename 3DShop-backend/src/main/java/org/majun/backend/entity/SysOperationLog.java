package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志实体类
 */
@Data
@TableName("sys_operation_log")
@Schema(description = "操作日志")
public class SysOperationLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 操作人ID
     */
    @TableField("operator_id")
    @Schema(description = "操作人ID")
    private Long operatorId;

    /**
     * 操作人姓名
     */
    @TableField("operator_name")
    @Schema(description = "操作人姓名")
    private String operatorName;

    /**
     * 操作类型
     */
    @TableField("operation_type")
    @Schema(description = "操作类型：CREATE/UPDATE/DELETE/REVIEW/LOGIN等")
    private String operationType;

    /**
     * 模块名称
     */
    @TableField("module")
    @Schema(description = "模块名称")
    private String module;

    /**
     * 操作描述
     */
    @TableField("description")
    @Schema(description = "操作描述")
    private String description;

    /**
     * 操作对象类型
     */
    @TableField("target_type")
    @Schema(description = "操作对象类型：USER/MODEL/ORDER等")
    private String targetType;

    /**
     * 操作对象ID
     */
    @TableField("target_id")
    @Schema(description = "操作对象ID")
    private Long targetId;

    /**
     * 操作内容
     */
    @TableField("content")
    @Schema(description = "操作内容")
    private String content;

    /**
     * 变更前数据
     */
    @TableField("before_data")
    @Schema(description = "变更前数据(JSON)")
    private String beforeData;

    /**
     * 变更后数据
     */
    @TableField("after_data")
    @Schema(description = "变更后数据(JSON)")
    private String afterData;

    /**
     * 操作IP
     */
    @TableField("ip")
    @Schema(description = "操作IP")
    private String ip;

    /**
     * 请求路径
     */
    @TableField("request_url")
    @Schema(description = "请求路径")
    private String requestUrl;

    /**
     * 请求方法
     */
    @TableField("request_method")
    @Schema(description = "请求方法")
    private String requestMethod;

    /**
     * 是否成功
     */
    @TableField("success")
    @Schema(description = "是否成功：1-成功, 0-失败")
    private Integer success;

    /**
     * 错误信息
     */
    @TableField("error_msg")
    @Schema(description = "错误信息")
    private String errorMsg;

    /**
     * 执行时长(毫秒)
     */
    @TableField("duration")
    @Schema(description = "执行时长(毫秒)")
    private Long duration;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
