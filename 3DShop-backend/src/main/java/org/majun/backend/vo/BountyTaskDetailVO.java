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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<String> attachments;
    private Long pendingDeliveryId;
    private List<BountyBidVO> bids;
}
