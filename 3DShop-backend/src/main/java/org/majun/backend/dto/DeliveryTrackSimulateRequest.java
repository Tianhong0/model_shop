package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Delivery track simulation request")
public class DeliveryTrackSimulateRequest {

    @NotNull(message = "Delivery ID is required")
    @Schema(description = "Delivery ID")
    private Long deliveryId;

    @Schema(description = "Simulation start time")
    private LocalDateTime startTime;
}
