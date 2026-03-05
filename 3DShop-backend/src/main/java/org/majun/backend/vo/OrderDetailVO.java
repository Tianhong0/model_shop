package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order detail.
 */
@Data
@Schema(description = "Order detail")
public class OrderDetailVO {

    @Schema(description = "Order ID")
    private Long id;

    @Schema(description = "Order serial number")
    private String orderSn;

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "Model ID")
    private Long modelId;

    @Schema(description = "Model name")
    private String modelName;

    @Schema(description = "Model main image")
    private String mainImageUrl;

    @Schema(description = "Material ID")
    private Long materialId;

    @Schema(description = "Material name")
    private String materialName;

    @Schema(description = "Material color")
    private String materialColor;

    @Schema(description = "Order price")
    private BigDecimal orderPrice;

    @Schema(description = "Order status")
    private Integer orderStatus;

    @Schema(description = "Printer ID")
    private Long printerId;

    @Schema(description = "Custom params JSON")
    private String customParams;

    @Schema(description = "Create time")
    private LocalDateTime createTime;

    @Schema(description = "Update time")
    private LocalDateTime updateTime;
}
