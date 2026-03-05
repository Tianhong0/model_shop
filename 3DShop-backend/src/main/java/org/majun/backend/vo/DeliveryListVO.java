package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Delivery list item")
public class DeliveryListVO {

    @Schema(description = "Delivery ID")
    private Long id;

    @Schema(description = "Order ID")
    private Long orderId;

    @Schema(description = "Order serial number")
    private String orderSn;

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "Delivery company")
    private String deliveryCompany;

    @Schema(description = "Delivery serial number")
    private String deliverySn;

    @Schema(description = "Delivery status")
    private Integer status;

    @Schema(description = "Delivery time")
    private LocalDateTime deliveryTime;

    @Schema(description = "Receive time")
    private LocalDateTime receiveTime;

    @Schema(description = "Create time")
    private LocalDateTime createTime;
}
