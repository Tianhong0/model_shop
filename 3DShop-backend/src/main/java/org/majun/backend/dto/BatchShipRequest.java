package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量发货请求
 */
@Data
@Schema(description = "批量发货请求")
public class BatchShipRequest {

    @NotEmpty(message = "订单编号列表不能为空")
    @Schema(description = "订单编号列表")
    private List<String> orderSns;

    @NotBlank(message = "物流公司不能为空")
    @Schema(description = "物流公司")
    private String deliveryCompany;

    @Schema(description = "物流单号列表（与orderSns一一对应，可为空则自动生成模拟单号）")
    private List<String> deliverySns;

    @Schema(description = "是否自动生成物流单号")
    private Boolean autoGenerateSn = true;
}
