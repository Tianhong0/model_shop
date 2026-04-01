package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志查询请求
 */
@Data
@Schema(description = "操作日志查询请求")
public class OperationLogQueryRequest {

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "操作人姓名(模糊查询)")
    private String operatorName;

    @Schema(description = "操作人ID(精确查询)")
    private Long operatorId;

    @Schema(description = "操作类型")
    private String operationType;

    @Schema(description = "模块名称")
    private String module;

    @Schema(description = "操作对象类型")
    private String targetType;

    @Schema(description = "是否成功：1-成功, 0-失败")
    private Integer success;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}
