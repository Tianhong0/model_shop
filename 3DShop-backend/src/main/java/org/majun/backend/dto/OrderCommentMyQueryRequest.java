package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "My comment query request")
public class OrderCommentMyQueryRequest {

    @Min(value = 1, message = "Page number must be >= 1")
    @Schema(description = "Page number")
    private Integer pageNum = 1;

    @Min(value = 1, message = "Page size must be >= 1")
    @Schema(description = "Page size")
    private Integer pageSize = 10;

    @Schema(description = "Order ID")
    private Long orderId;

    @Schema(description = "Model ID")
    private Long modelId;
}
