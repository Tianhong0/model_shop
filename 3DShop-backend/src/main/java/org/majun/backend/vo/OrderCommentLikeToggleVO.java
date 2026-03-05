package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单评价点赞切换结果")
public class OrderCommentLikeToggleVO {

    @Schema(description = "评价ID")
    private Long commentId;

    @Schema(description = "切换后是否点赞")
    private Boolean active;
}
