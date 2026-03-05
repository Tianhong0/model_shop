package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "钱包充值支付宝下单响应")
public class WalletRechargePayCreateResponse {

    @Schema(description = "支付宝支付单号")
    private String outTradeNo;

    @Schema(description = "充值金额")
    private BigDecimal amount;

    @Schema(description = "支付宝订单字符串")
    private String orderString;
}
