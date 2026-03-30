package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 发起拼团请求
 */
@Data
@Schema(description = "发起拼团请求")
public class GroupBuyCreateRequest {

    @NotNull(message = "活动ID不能为空")
    @Schema(description = "活动ID")
    private Long activityId;

    @Schema(description = "材质ID")
    private Long materialId;

    @Schema(description = "颜色")
    private String color;

    @DecimalMin(value = "0.01", message = "缩放比例必须大于0")
    @Schema(description = "缩放比例")
    private BigDecimal scale;

    @DecimalMin(value = "0", message = "填充密度必须>=0")
    @DecimalMax(value = "100", message = "填充密度必须<=100")
    @Schema(description = "填充密度")
    private BigDecimal fillPercent;

    @Min(value = 1, message = "购买数量必须>=1")
    @Schema(description = "购买数量")
    private Integer quantity = 1;

    @Schema(description = "备注")
    private String note;

    @Schema(description = "使用积分")
    private Integer usePoints;

    @Schema(description = "打印精度(层高)")
    private BigDecimal precision;

    @Schema(description = "耗材线径")
    private BigDecimal filamentDiameter;

    @Schema(description = "其他定制参数JSON")
    private String customParams;
}
