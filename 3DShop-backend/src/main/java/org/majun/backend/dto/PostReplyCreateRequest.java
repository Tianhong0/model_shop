package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建回复请求")
public class PostReplyCreateRequest {

    @NotNull(message = "帖子ID不能为空")
    @Schema(description = "帖子ID")
    private Long postId;

    @Schema(description = "父回复ID，顶级回复传0")
    private Long parentId = 0L;

    @NotBlank(message = "回复内容不能为空")
    @Schema(description = "回复内容")
    private String content;
}
