package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "后台更新帖子置顶状态")
public class PostTopUpdateRequest {

    @NotNull(message = "帖子ID不能为空")
    @Schema(description = "帖子ID")
    private Long postId;

    @NotNull(message = "置顶状态不能为空")
    @Schema(description = "置顶: 0否/1是")
    private Integer isTop;
}
