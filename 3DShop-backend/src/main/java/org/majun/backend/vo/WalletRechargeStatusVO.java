package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "钱包充值支付状态")
public class WalletRechargeStatusVO {

    @Schema(description = "支付宝支付单号")
    private String outTradeNo;

    @Schema(description = "状态:0待支付,1已支付,2已关闭")
    private Integer status;

    @Schema(description = "支付金额")
    private BigDecimal amount;

    @Schema(description = "支付宝交易号")
    private String tradeNo;

    @Schema(description = "支付完成时间")
    private LocalDateTime payTime;
}
