package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Delivery ship request")
public class DeliveryShipRequest {

    @NotBlank(message = "Order serial number is required")
    @Schema(description = "Order serial number")
    private String orderSn;

    @NotBlank(message = "Delivery company is required")
    @Schema(description = "Delivery company")
    private String deliveryCompany;

    @NotBlank(message = "Delivery serial number is required")
    @Schema(description = "Delivery serial number")
    private String deliverySn;

    @NotBlank(message = "Receiver name is required")
    @Schema(description = "Receiver name")
    private String receiverName;

    @NotBlank(message = "Receiver phone is required")
    @Schema(description = "Receiver phone")
    private String receiverPhone;

    @NotBlank(message = "Receiver address is required")
    @Schema(description = "Receiver address")
    private String receiverAddress;
}
