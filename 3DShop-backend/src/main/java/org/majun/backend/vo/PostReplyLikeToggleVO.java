package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "回复点赞切换结果")
public class PostReplyLikeToggleVO {

    @Schema(description = "回复ID")
    private Long replyId;

    @Schema(description = "切换后是否点赞")
    private Boolean active;
}
