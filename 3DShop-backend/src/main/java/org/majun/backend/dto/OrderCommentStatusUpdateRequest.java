package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Order comment status update request")
public class OrderCommentStatusUpdateRequest {

    @NotNull(message = "Comment ID is required")
    @Schema(description = "Comment ID")
    private Long commentId;

    @NotNull(message = "Status is required")
    @Schema(description = "Status: 1 normal, 0 hidden")
    private Integer status;
}
