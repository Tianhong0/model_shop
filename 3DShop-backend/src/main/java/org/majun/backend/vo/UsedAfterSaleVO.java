package org.majun.backend.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UsedAfterSaleVO {

    private Long id;
    private String afterSaleSn;
    private Long orderId;
    private String orderSn;
    private Integer type;
    private String reason;
    private String description;
    private String evidenceUrls;
    private BigDecimal requestedAmount;
    private BigDecimal refundAmount;
    private Integer status;
    private String sellerRemark;
    private String adminRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
