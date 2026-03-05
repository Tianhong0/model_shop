package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "After-sale create request")
public class AfterSaleCreateRequest {

    @NotNull(message = "Order ID is required")
    @Schema(description = "Order ID")
    private Long orderId;

    @NotNull(message = "After-sale type is required")
    @Schema(description = "After-sale type: 1-refund only,2-return refund,3-reprint,4-exchange")
    private Integer type;

    @NotBlank(message = "Reason is required")
    @Schema(description = "Reason")
    private String reason;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Evidence URLs separated by comma")
    private String evidenceUrls;

    @DecimalMin(value = "0", message = "Requested amount must be >= 0")
    @Schema(description = "Requested refund amount")
    private BigDecimal requestedAmount;
}
