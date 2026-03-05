package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Order comment reply request")
public class OrderCommentReplyRequest {

    @NotNull(message = "Comment ID is required")
    @Schema(description = "Comment ID")
    private Long commentId;

    @NotBlank(message = "Reply content is required")
    @Schema(description = "Reply content")
    private String replyContent;
}
