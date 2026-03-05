package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Order query request.
 */
@Data
@Schema(description = "Order query request")
public class OrderQueryRequest {

    @Min(value = 1, message = "Page number must be >= 1")
    @Schema(description = "Page number")
    private Integer pageNum = 1;

    @Min(value = 1, message = "Page size must be >= 1")
    @Schema(description = "Page size")
    private Integer pageSize = 10;

    @Schema(description = "Order status")
    private Integer orderStatus;

    @Schema(description = "Order serial number")
    private String orderSn;

    @Schema(description = "Model ID")
    private Long modelId;

    @Schema(description = "User ID (admin only)")
    private Long userId;
}
