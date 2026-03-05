package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "提现打款")
public class WalletWithdrawPayRequest {

    @NotNull(message = "提现记录ID不能为空")
    private Long withdrawId;

    @NotNull(message = "打款结果不能为空")
    @Schema(description = "打款结果:1成功,2失败")
    private Integer payResult;

    @Schema(description = "打款备注")
    private String remark;
}
