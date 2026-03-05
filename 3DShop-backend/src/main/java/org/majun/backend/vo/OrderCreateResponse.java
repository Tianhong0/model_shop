package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Order create response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Order create response")
public class OrderCreateResponse {

    @Schema(description = "Order ID")
    private Long orderId;

    @Schema(description = "Order serial number")
    private String orderSn;

    @Schema(description = "Order price")
    private BigDecimal orderPrice;

    @Schema(description = "Base model price")
    private BigDecimal basePrice;

    @Schema(description = "Material cost")
    private BigDecimal materialCost;

    @Schema(description = "Goods total amount")
    private BigDecimal goodsAmount;

    @Schema(description = "Shipping fee")
    private BigDecimal shippingFee;

    @Schema(description = "Discount amount")
    private BigDecimal discountAmount;

    @Schema(description = "Final pay amount")
    private BigDecimal payAmount;

    @Schema(description = "使用积分数量")
    private Integer usedPoints;

    @Schema(description = "积分抵扣金额")
    private BigDecimal pointDiscountAmount;
}
