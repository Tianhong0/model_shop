package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "After-sale audit request")
public class AfterSaleAuditRequest {

    @NotNull(message = "After-sale ID is required")
    private Long afterSaleId;

    @NotNull(message = "Approved flag is required")
    private Boolean approved;

    @DecimalMin(value = "0", message = "Approved amount must be >= 0")
    @Schema(description = "Approved amount for refund types")
    private BigDecimal approvedAmount;

    @Schema(description = "Admin remark")
    private String adminRemark;
}
