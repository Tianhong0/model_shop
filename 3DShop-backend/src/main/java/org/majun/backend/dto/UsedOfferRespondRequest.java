package org.majun.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UsedOfferRespondRequest {

    @NotNull(message = "议价ID不能为空")
    private Long offerId;

    @NotNull(message = "处理结果不能为空")
    private Boolean approved;

    private String remark;
}
