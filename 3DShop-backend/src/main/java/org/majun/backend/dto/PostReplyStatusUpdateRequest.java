package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "后台更新回复状态")
public class PostReplyStatusUpdateRequest {

    @NotNull(message = "回复ID不能为空")
    @Schema(description = "回复ID")
    private Long replyId;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态: 1正常/0屏蔽")
    private Integer status;
}
