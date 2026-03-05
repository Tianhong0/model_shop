package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "User delivery query request")
public class UserDeliveryQueryRequest {

    @NotBlank(message = "Order serial number is required")
    @Schema(description = "Order serial number")
    private String orderSn;
}
