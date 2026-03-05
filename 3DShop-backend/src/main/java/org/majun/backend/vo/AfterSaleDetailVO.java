package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "After-sale detail")
public class AfterSaleDetailVO {

    private Long id;
    private String afterSaleSn;
    private Long orderId;
    private String orderSn;
    private Long userId;
    private Integer type;
    private String reason;
    private String description;
    private String evidenceUrls;
    private BigDecimal requestedAmount;
    private BigDecimal approvedAmount;
    private Integer status;
    private Integer refundStatus;
    private String adminRemark;
    private String closeReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<AfterSaleMessageVO> latestMessages;
}
