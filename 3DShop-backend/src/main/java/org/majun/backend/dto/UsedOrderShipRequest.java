package org.majun.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UsedOrderShipRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotBlank(message = "物流公司不能为空")
    private String deliveryCompany;

    @NotBlank(message = "物流单号不能为空")
    private String deliverySn;
}
