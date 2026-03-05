package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Order payment status")
public class OrderPayStatusVO {

    @Schema(description = "Order ID")
    private Long orderId;

    @Schema(description = "Order serial number")
    private String orderSn;

    @Schema(description = "Order status")
    private Integer orderStatus;

    @Schema(description = "Payment status")
    private Integer payStatus;

    @Schema(description = "Merchant trade number")
    private String outTradeNo;

    @Schema(description = "Alipay trade number")
    private String tradeNo;

    @Schema(description = "Payment time")
    private LocalDateTime payTime;
}
