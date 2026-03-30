package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 参与拼团响应
 */
@Data
@Builder
@Schema(description = "参与拼团响应")
public class GroupBuyJoinResponse {

    @Schema(description = "拼团组ID")
    private Long groupId;

    @Schema(description = "参与ID")
    private Long participantId;

    @Schema(description = "单价")
    private BigDecimal unitPrice;

    @Schema(description = "总金额")
    private BigDecimal totalAmount;

    @Schema(description = "当前人数")
    private Integer currentPeople;

    @Schema(description = "目标人数")
    private Integer targetPeople;
}
