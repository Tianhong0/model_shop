package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Delivery track add request")
public class DeliveryTrackAddRequest {

    @NotNull(message = "Delivery ID is required")
    @Schema(description = "Delivery ID")
    private Long deliveryId;

    @NotBlank(message = "Track content is required")
    @Schema(description = "Track content")
    private String trackContent;

    @Schema(description = "Track time")
    private LocalDateTime trackTime;

    @Schema(description = "Operator info")
    private String operatorInfo;
}
