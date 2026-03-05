package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "钱包账户概览")
public class WalletAccountVO {

    private String userId;

    private BigDecimal availableBalance;

    private BigDecimal frozenBalance;

    private Integer status;
}
