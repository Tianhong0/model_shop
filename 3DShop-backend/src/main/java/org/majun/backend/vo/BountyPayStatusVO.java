package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "悬赏补差支付状态")
public class BountyPayStatusVO {

    @Schema(description = "改价ID")
    private Long priceChangeId;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "当前改价状态")
    private Integer priceChangeStatus;

    @Schema(description = "任务当前金额")
    private BigDecimal currentTaskAmount;

    @Schema(description = "改价目标金额")
    private BigDecimal targetAmount;

    @Schema(description = "是否需要补差支付")
    private Boolean needPay;

    @Schema(description = "是否已支付完成")
    private Boolean paid;

    @Schema(description = "商户支付单号")
    private String outTradeNo;
}
