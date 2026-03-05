package org.majun.backend.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BountyBidVO {

    private Long id;
    private Long taskId;
    private Long designerId;
    private BigDecimal quoteAmount;
    private Integer deliveryDays;
    private String proposal;
    private Integer status;
    private LocalDateTime createTime;
}
