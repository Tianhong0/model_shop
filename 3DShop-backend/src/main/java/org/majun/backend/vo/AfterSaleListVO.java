package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "After-sale list item")
public class AfterSaleListVO {

    private Long id;
    private String afterSaleSn;
    private Long orderId;
    private String orderSn;
    private Integer type;
    private String reason;
    private Integer status;
    private Integer refundStatus;
    private BigDecimal requestedAmount;
    private BigDecimal approvedAmount;
    private String adminRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
