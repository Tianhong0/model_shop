package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "交互切换结果")
public class PostInteractionToggleVO {

    @Schema(description = "帖子ID")
    private Long postId;

    @Schema(description = "交互类型: 1点赞/2收藏")
    private Integer interactType;

    @Schema(description = "切换后是否生效")
    private Boolean active;
}
