package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Order app pay create response")
public class OrderPayCreateResponse {

    @Schema(description = "Order ID")
    private Long orderId;

    @Schema(description = "Order serial number")
    private String orderSn;

    @Schema(description = "Merchant trade number")
    private String outTradeNo;

    @Schema(description = "Amount")
    private BigDecimal amount;

    @Schema(description = "Alipay order string for app SDK")
    private String orderString;
}
