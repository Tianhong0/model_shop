package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "订单评价追评分页查询请求")
public class OrderCommentReplyQueryRequest {

    @NotNull(message = "评价ID不能为空")
    @Schema(description = "评价ID")
    private Long commentId;

    @Min(value = 1, message = "页码必须大于等于1")
    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数必须大于等于1")
    @Schema(description = "每页条数")
    private Integer pageSize = 10;
}
