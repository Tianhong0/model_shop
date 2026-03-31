package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 打印故障诊断结果VO
 */
@Data
@Schema(description = "打印故障诊断结果")
public class PrintFaultDiagnosisVO {

    @Schema(description = "故障代码")
    private String faultCode;

    @Schema(description = "故障分类")
    private String faultCategory;

    @Schema(description = "故障分类名称")
    private String faultCategoryName;

    @Schema(description = "故障名称")
    private String faultName;

    @Schema(description = "故障描述")
    private String description;

    @Schema(description = "处理建议列表")
    private List<String> suggestions;

    @Schema(description = "原始错误信息")
    private String errorMessage;

    @Schema(description = "是否可重试")
    private Boolean canRetry;

    @Schema(description = "诊断时间")
    private LocalDateTime diagnoseTime;

    @Schema(description = "已重试次数")
    private Integer retryCount;

    @Schema(description = "最大重试次数")
    private Integer maxRetryCount;
}
