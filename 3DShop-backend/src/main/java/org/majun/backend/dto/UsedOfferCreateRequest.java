package org.majun.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UsedOfferCreateRequest {

    @NotNull(message = "商品ID不能为空")
    private Long listingId;

    @NotNull(message = "出价不能为空")
    @DecimalMin(value = "0.01", message = "出价必须大于0")
    private BigDecimal offerAmount;

    private String remark;
}
