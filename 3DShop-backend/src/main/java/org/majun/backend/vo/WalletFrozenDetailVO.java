package org.majun.backend.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包冻结详情VO
 */
@Data
public class WalletFrozenDetailVO {

    private String id;

    private BigDecimal amount;

    private String bizType;

    private String bizNo;

    private Integer frozenDays;

    private LocalDateTime frozenStartTime;

    private LocalDateTime frozenEndTime;

    /**
     * 剩余解冻天数
     */
    private Long remainingDays;

    /**
     * 剩余解冻小时
     */
    private Long remainingHours;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;
}
