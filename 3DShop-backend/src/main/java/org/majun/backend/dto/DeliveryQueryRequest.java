package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "Delivery query request")
public class DeliveryQueryRequest {

    @Min(value = 1, message = "Page number must be >= 1")
    @Schema(description = "Page number")
    private Integer pageNum = 1;

    @Min(value = 1, message = "Page size must be >= 1")
    @Schema(description = "Page size")
    private Integer pageSize = 10;

    @Schema(description = "Order serial number")
    private String orderSn;

    @Schema(description = "Delivery serial number")
    private String deliverySn;

    @Schema(description = "Delivery company")
    private String deliveryCompany;

    @Schema(description = "Delivery status")
    private Integer status;
}
