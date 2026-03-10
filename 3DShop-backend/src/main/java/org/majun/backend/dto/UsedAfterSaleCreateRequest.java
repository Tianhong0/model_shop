package org.majun.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UsedAfterSaleCreateRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotNull(message = "售后类型不能为空")
    private Integer type;

    @NotBlank(message = "售后原因不能为空")
    private String reason;

    private String description;

    private String evidenceUrls;

    @DecimalMin(value = "0.00", message = "退款金额不能小于0")
    private BigDecimal requestedAmount;
}
