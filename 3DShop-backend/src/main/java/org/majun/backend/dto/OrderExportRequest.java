package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 订单导出请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "订单导出请求")
public class OrderExportRequest extends ExportRequest {

    @Schema(description = "指定导出的订单ID列表（优先使用）")
    private List<Long> orderIds;

    @Schema(description = "订单状态列表")
    private List<Integer> orderStatuses;

    @Schema(description = "订单编号（模糊搜索）")
    private String orderSn;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "支付方式")
    private String paymentMethod;
}
