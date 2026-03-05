package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "悬赏补差支付创建响应")
public class BountyPayCreateResponse {

    @Schema(description = "改价ID")
    private Long priceChangeId;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "商户支付单号")
    private String outTradeNo;

    @Schema(description = "补差金额")
    private BigDecimal amount;

    @Schema(description = "支付宝App支付串")
    private String orderString;
}
