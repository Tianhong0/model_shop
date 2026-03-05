package org.majun.backend.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PrintJobProgressVO {
    private Long jobId;
    private Long orderId;
    private Long printerId;
    private Integer status;
    private String statusDesc;
    private BigDecimal progress;
    private BigDecimal toolTempActual;
    private BigDecimal toolTempTarget;
    private BigDecimal bedTempActual;
    private BigDecimal bedTempTarget;
    private Integer estimatedSecondsLeft;
    private String errorMessage;
    private Long timestamp;
}
