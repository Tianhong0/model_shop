package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "提现申请")
public class WalletWithdrawApplyRequest {

    @NotNull(message = "提现金额不能为空")
    @DecimalMin(value = "0.01", message = "提现金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "申请备注")
    private String remark;

    @NotBlank(message = "支付宝收款账号不能为空")
    @Schema(description = "支付宝收款账号(沙箱买家账号)")
    private String alipayAccount;

    @Schema(description = "支付宝收款人姓名(选填)")
    private String alipayRealName;
}
