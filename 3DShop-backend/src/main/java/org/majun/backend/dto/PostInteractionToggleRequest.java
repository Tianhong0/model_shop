package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "帖子交互切换请求")
public class PostInteractionToggleRequest {

    @NotNull(message = "帖子ID不能为空")
    @Schema(description = "帖子ID")
    private Long postId;

    @NotNull(message = "交互类型不能为空")
    @Schema(description = "交互类型: 1-点赞, 2-收藏")
    private Integer interactType;
}
