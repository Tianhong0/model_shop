package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "改价确认请求")
public class BountyPriceChangeConfirmRequest {

    @NotNull(message = "改价ID不能为空")
    private Long priceChangeId;

    @NotNull(message = "确认状态不能为空")
    @Schema(description = "确认结果:1同意,2拒绝")
    private Integer decision;
}
