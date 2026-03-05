package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "订单评价追评点赞切换请求")
public class OrderCommentReplyLikeToggleRequest {

    @NotNull(message = "追评ID不能为空")
    @Schema(description = "追评ID")
    private Long replyId;
}
