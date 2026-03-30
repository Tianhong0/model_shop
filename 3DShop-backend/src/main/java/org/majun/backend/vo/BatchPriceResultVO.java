package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 批量打印价格结果VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "批量打印价格结果VO")
public class BatchPriceResultVO {

    @Schema(description = "原始单价")
    private BigDecimal originalUnitPrice;

    @Schema(description = "折扣后单价")
    private BigDecimal discountedUnitPrice;

    @Schema(description = "折扣百分比")
    private BigDecimal discountPercent;

    @Schema(description = "购买数量")
    private Integer quantity;

    @Schema(description = "总金额")
    private BigDecimal totalAmount;

    @Schema(description = "节省金额")
    private BigDecimal savedAmount;

    @Schema(description = "适用的折扣规则描述")
    private String discountRule;
}
