package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "帖子回复点赞切换请求")
public class PostReplyLikeToggleRequest {

    @NotNull(message = "回复ID不能为空")
    @Schema(description = "回复ID")
    private Long replyId;
}
