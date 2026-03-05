package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Order app pay create request")
public class OrderPayCreateRequest {

    @NotNull(message = "Order ID is required")
    @Schema(description = "Order ID")
    private Long orderId;
}
