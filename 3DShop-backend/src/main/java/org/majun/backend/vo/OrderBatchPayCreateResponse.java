package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Order batch app pay create response")
public class OrderBatchPayCreateResponse {

    @Schema(description = "Batch ID")
    private Long batchId;

    @Schema(description = "Merchant trade number")
    private String outTradeNo;

    @Schema(description = "Amount")
    private BigDecimal amount;

    @Schema(description = "Order IDs")
    private List<Long> orderIds;

    @Schema(description = "Alipay order string for app SDK")
    private String orderString;
}
