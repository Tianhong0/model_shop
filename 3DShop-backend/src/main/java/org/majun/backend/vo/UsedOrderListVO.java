package org.majun.backend.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UsedOrderListVO {

    private Long id;
    private String orderSn;
    private Long listingId;
    private String listingTitle;
    private String coverUrl;
    private Long buyerId;
    private Long sellerId;
    private String buyerNickname;
    private String sellerNickname;
    private BigDecimal orderAmount;
    private Integer status;
    private Long afterSaleId;
    private Integer afterSaleStatus;
    private String deliveryCompany;
    private String deliverySn;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
}
