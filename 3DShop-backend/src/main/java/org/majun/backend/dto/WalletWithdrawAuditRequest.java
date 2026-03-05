package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "提现审核")
public class WalletWithdrawAuditRequest {

    @NotNull(message = "提现记录ID不能为空")
    private Long withdrawId;

    @NotNull(message = "审核结果不能为空")
    @Schema(description = "审核结果:1通过,2拒绝")
    private Integer decision;

    @Schema(description = "审核备注")
    private String remark;
}
