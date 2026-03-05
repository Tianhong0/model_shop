package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Create order comment request")
public class OrderCommentCreateRequest {

    @NotNull(message = "Order ID is required")
    @Schema(description = "Order ID")
    private Long orderId;

    @NotNull(message = "Model score is required")
    @Min(value = 1, message = "Model score must be between 1 and 5")
    @Max(value = 5, message = "Model score must be between 1 and 5")
    @Schema(description = "Model score")
    private Integer modelScore;

    @NotNull(message = "Print score is required")
    @Min(value = 1, message = "Print score must be between 1 and 5")
    @Max(value = 5, message = "Print score must be between 1 and 5")
    @Schema(description = "Print score")
    private Integer printScore;

    @NotNull(message = "Service score is required")
    @Min(value = 1, message = "Service score must be between 1 and 5")
    @Max(value = 5, message = "Service score must be between 1 and 5")
    @Schema(description = "Service score")
    private Integer serviceScore;

    @Schema(description = "Comment text")
    private String commentText;

    @Schema(description = "Comment images, comma separated")
    private String commentImages;

    @Schema(description = "Is anonymous: 1 yes, 0 no")
    private Integer isAnonymous;
}
