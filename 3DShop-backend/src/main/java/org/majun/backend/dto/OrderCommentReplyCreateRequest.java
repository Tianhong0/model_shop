package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "订单评价追评创建请求")
public class OrderCommentReplyCreateRequest {

    @NotNull(message = "评价ID不能为空")
    @Schema(description = "评价ID")
    private Long commentId;

    @NotBlank(message = "追评内容不能为空")
    @Size(max = 1000, message = "追评内容最多1000字符")
    @Schema(description = "追评内容")
    private String content;
}
