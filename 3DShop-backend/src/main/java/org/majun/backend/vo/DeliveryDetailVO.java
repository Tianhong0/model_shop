package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "Delivery detail")
public class DeliveryDetailVO {

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

    @Schema(description = "Receiver name")
    private String receiverName;

    @Schema(description = "Receiver phone")
    private String receiverPhone;

    @Schema(description = "Receiver address")
    private String receiverAddress;

    @Schema(description = "Delivery status")
    private Integer status;

    @Schema(description = "Delivery time")
    private LocalDateTime deliveryTime;

    @Schema(description = "Receive time")
    private LocalDateTime receiveTime;

    @Schema(description = "Track list")
    private List<DeliveryTrackVO> tracks;
}
