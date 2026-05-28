package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "设计师分润结算响应")
public class DesignerSettlementVO {

    @Schema(description = "结算ID")
    private Long id;

    @Schema(description = "结算流水号")
    private String settlementSn;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单编号")
    private String orderSn;

    @Schema(description = "模型ID")
    private Long modelId;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "设计师ID")
    private Long designerId;

    @Schema(description = "设计师名称")
    private String designerName;

    @Schema(description = "订单金额")
    private BigDecimal orderPrice;

    @Schema(description = "分润比例")
    private Integer profitShareRatio;

    @Schema(description = "结算金额")
    private BigDecimal settlementAmount;

    @Schema(description = "结算状态: 0-待结算, 1-已结算, 2-结算失败")
    private Integer status;

    @Schema(description = "关联钱包流水ID")
    private Long walletLedgerId;

    @Schema(description = "创建时间")
    private String createTime;
}
