package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Delivery status update request")
public class DeliveryStatusUpdateRequest {

    @NotNull(message = "Delivery ID is required")
    @Schema(description = "Delivery ID")
    private Long deliveryId;

    @NotNull(message = "Status is required")
    @Schema(description = "Delivery status")
    private Integer status;

    @Schema(description = "Track content")
    private String trackContent;

    @Schema(description = "Operator info")
    private String operatorInfo;
}
