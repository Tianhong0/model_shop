package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "提现记录")
public class WalletWithdrawVO {

    private String id;

    private String userId;

    private String withdrawSn;

    private BigDecimal amount;

    private Integer status;

    private String applyRemark;

    private String alipayAccount;

    private String alipayRealName;

    private String auditRemark;

    private String payRemark;

    private String auditBy;

    private LocalDateTime auditTime;

    private String payBy;

    private LocalDateTime payTime;

    private LocalDateTime createTime;
}
