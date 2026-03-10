package org.majun.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UsedAfterSaleAuditRequest {

    @NotNull(message = "售后单ID不能为空")
    private Long afterSaleId;

    @NotNull(message = "处理结果不能为空")
    private Boolean approved;

    @DecimalMin(value = "0.00", message = "退款金额不能小于0")
    private BigDecimal refundAmount;

    private String remark;
}
