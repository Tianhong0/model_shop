package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "修改竞标请求")
public class BountyBidUpdateRequest {

    @NotNull(message = "竞标ID不能为空")
    private Long bidId;

    @NotNull(message = "报价不能为空")
    @DecimalMin(value = "0.01", message = "报价必须大于0")
    private BigDecimal quoteAmount;

    @Schema(description = "承诺交付天数")
    private Integer deliveryDays;

    @NotBlank(message = "竞标方案不能为空")
    private String proposal;
}
