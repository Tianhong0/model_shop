package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Retry auto ship request")
public class RetryAutoShipRequest {

    @NotNull(message = "Order ID is required")
    @Schema(description = "Order ID")
    private Long orderId;

    @Schema(description = "Receiver name (optional, will extract from order if not provided)")
    private String receiverName;

    @Schema(description = "Receiver phone (optional, will extract from order if not provided)")
    private String receiverPhone;

    @Schema(description = "Receiver address (optional, will extract from order if not provided)")
    private String receiverAddress;
}
