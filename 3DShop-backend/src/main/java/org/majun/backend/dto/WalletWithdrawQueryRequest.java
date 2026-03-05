package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "提现记录分页查询")
public class WalletWithdrawQueryRequest {

    @Min(value = 1, message = "页码必须大于等于1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页大小必须大于等于1")
    private Integer pageSize = 10;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "用户ID(管理员筛选)")
    private Long userId;
}
