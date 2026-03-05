package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "订单评价点赞切换请求")
public class OrderCommentLikeToggleRequest {

    @NotNull(message = "评价ID不能为空")
    @Schema(description = "评价ID")
    private Long commentId;
}
