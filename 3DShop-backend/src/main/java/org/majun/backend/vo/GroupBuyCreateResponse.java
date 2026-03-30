package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 发起拼团响应
 */
@Data
@Builder
@Schema(description = "发起拼团响应")
public class GroupBuyCreateResponse {

    @Schema(description = "拼团组ID")
    private Long groupId;

    @Schema(description = "拼团编号")
    private String groupSn;

    @Schema(description = "分享码")
    private String shareCode;

    @Schema(description = "参与ID")
    private Long participantId;

    @Schema(description = "单价")
    private BigDecimal unitPrice;

    @Schema(description = "总金额")
    private BigDecimal totalAmount;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;
}
