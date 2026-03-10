package org.majun.backend.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UsedOfferVO {

    private Long id;
    private Long buyerId;
    private String buyerNickname;
    private BigDecimal offerAmount;
    private String remark;
    private Integer status;
    private LocalDateTime createTime;
}
