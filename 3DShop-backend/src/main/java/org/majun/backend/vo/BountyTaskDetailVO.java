package org.majun.backend.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BountyTaskDetailVO {

    private Long id;
    private String taskSn;
    private Long publisherId;
    private String title;
    private String description;
    private String category;
    private String tags;
    private BigDecimal budgetAmount;
    private BigDecimal finalAmount;
    private Integer expectedDays;
    private LocalDateTime deadlineTime;
    private Integer status;
    private Long winnerBidId;
    private Long winnerDesignerId;
    private String closeReason;
    private Integer cancelRequested;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<String> attachments;
    private Long pendingDeliveryId;
    private List<BountyBidVO> bids;
    private DeliveryInfo deliveryInfo;

    @Data
    public static class DeliveryInfo {
        private Long id;
        private Integer deliveryRound;
        private String description;
        private Integer status;
        private Integer isFinal;
        private Integer allowCommercialUse;
        private Integer allowModification;
        private String licenseType;
        private List<DeliveryFileItem> files;
        private LocalDateTime createTime;
    }

    @Data
    public static class DeliveryFileItem {
        private String url;
        private String name;
        private String type;
    }
}
