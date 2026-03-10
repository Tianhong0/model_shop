package org.majun.backend.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UsedOrderDetailVO {

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
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String deliveryCompany;
    private String deliverySn;
    private LocalDateTime deliveryTime;
    private LocalDateTime receiveTime;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private String cancelReason;
    private UsedAfterSaleVO afterSale;
}
