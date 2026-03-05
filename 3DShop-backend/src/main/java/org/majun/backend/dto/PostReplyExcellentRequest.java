package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "后台设置优质回复")
public class PostReplyExcellentRequest {

    @NotNull(message = "回复ID不能为空")
    @Schema(description = "回复ID")
    private Long replyId;

    @NotNull(message = "优质状态不能为空")
    @Schema(description = "优质: 1是/0否")
    private Integer isExcellent;
}
