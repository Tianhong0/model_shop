package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "After-sale refund request")
public class AfterSaleRefundRequest {

    @NotNull(message = "After-sale ID is required")
    private Long afterSaleId;

    @NotNull(message = "Refund amount is required")
    @DecimalMin(value = "0.01", message = "Refund amount must be > 0")
    private BigDecimal refundAmount;

    @Schema(description = "Refund reason")
    private String refundReason;
}
