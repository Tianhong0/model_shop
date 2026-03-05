package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "Order batch payment status")
public class OrderBatchPayStatusVO {

    @Schema(description = "Batch ID")
    private Long batchId;

    @Schema(description = "Payment status")
    private Integer payStatus;

    @Schema(description = "Batch status")
    private Integer batchStatus;

    @Schema(description = "Merchant trade number")
    private String outTradeNo;

    @Schema(description = "Alipay trade number")
    private String tradeNo;

    @Schema(description = "Payment time")
    private LocalDateTime payTime;

    @Schema(description = "Order status list")
    private List<OrderStatusItem> orders;

    @Data
    @Schema(description = "Batch order status item")
    public static class OrderStatusItem {

        @Schema(description = "Order ID")
        private Long orderId;

        @Schema(description = "Order serial number")
        private String orderSn;

        @Schema(description = "Order status")
        private Integer orderStatus;
    }
}
