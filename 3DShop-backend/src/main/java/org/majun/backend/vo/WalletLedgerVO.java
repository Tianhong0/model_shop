package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "钱包流水")
public class WalletLedgerVO {

    private String id;

    private Integer direction;

    private String bizType;

    private String bizNo;

    private String refId;

    private BigDecimal amount;

    private BigDecimal beforeAvailable;

    private BigDecimal afterAvailable;

    private BigDecimal beforeFrozen;

    private BigDecimal afterFrozen;

    private String remark;

    private LocalDateTime createTime;
}
