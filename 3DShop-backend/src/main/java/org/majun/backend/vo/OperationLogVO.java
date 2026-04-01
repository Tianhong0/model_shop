package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志VO
 */
@Data
@Schema(description = "操作日志")
public class OperationLogVO {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "操作类型")
    private String operationType;

    @Schema(description = "模块名称")
    private String module;

    @Schema(description = "操作描述")
    private String description;

    @Schema(description = "操作对象类型")
    private String targetType;

    @Schema(description = "操作对象ID")
    private Long targetId;

    @Schema(description = "操作内容")
    private String content;

    @Schema(description = "变更前数据")
    private String beforeData;

    @Schema(description = "变更后数据")
    private String afterData;

    @Schema(description = "操作IP")
    private String ip;

    @Schema(description = "请求路径")
    private String requestUrl;

    @Schema(description = "请求方法")
    private String requestMethod;

    @Schema(description = "是否成功")
    private Integer success;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "执行时长(毫秒)")
    private Long duration;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
