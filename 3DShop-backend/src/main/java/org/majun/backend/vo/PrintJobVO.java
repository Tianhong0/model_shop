package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "打印任务")
public class PrintJobVO {
    private Long id;
    private Long orderId;
    private String orderSn;
    private Long modelId;
    private String modelName;
    private String modelFileName;
    private String modelFileType;
    private Integer status;
    private String statusDesc;
    private Integer priority;
    private Long printerId;
    private String printerName;
    private BigDecimal progress;
    private BigDecimal toolTempActual;
    private BigDecimal toolTempTarget;
    private BigDecimal bedTempActual;
    private BigDecimal bedTempTarget;
    private Integer estimatedSecondsLeft;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
