package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "后台更新帖子状态")
public class PostStatusUpdateRequest {

    @NotNull(message = "帖子ID不能为空")
    @Schema(description = "帖子ID")
    private Long postId;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态: 0草稿/1发布/2下架")
    private Integer status;
}
