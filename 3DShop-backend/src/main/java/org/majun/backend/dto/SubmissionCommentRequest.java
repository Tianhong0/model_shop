package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 作品评论请求
 */
@Data
@Schema(description = "作品评论请求")
public class SubmissionCommentRequest {

    @NotNull(message = "作品ID不能为空")
    @Schema(description = "作品ID")
    private Long submissionId;

    @Schema(description = "父评论ID(回复评论时)")
    private Long parentId;

    @Schema(description = "回复的用户ID")
    private Long replyToUserId;

    @NotBlank(message = "评论内容不能为空")
    @Schema(description = "评论内容")
    private String content;
}
