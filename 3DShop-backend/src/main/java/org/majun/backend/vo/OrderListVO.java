package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order list item.
 */
@Data
@Schema(description = "Order list item")
public class OrderListVO {

    @Schema(description = "Order ID")
    private Long id;

    @Schema(description = "Order serial number")
    private String orderSn;

    @Schema(description = "Model ID")
    private Long modelId;

    @Schema(description = "Model name")
    private String modelName;

    @Schema(description = "Model main image")
    private String mainImageUrl;

    @Schema(description = "Order price")
    private BigDecimal orderPrice;

    @Schema(description = "Order status")
    private Integer orderStatus;

    @Schema(description = "Create time")
    private LocalDateTime createTime;
}
