package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Order status update request.
 */
@Data
@Schema(description = "Order status update request")
public class OrderStatusUpdateRequest {

    @NotNull(message = "Order ID is required")
    @Schema(description = "Order ID")
    private Long orderId;

    @NotNull(message = "Status is required")
    @Schema(description = "Order status")
    private Integer status;

    @Schema(description = "Printer ID")
    private Long printerId;
}
