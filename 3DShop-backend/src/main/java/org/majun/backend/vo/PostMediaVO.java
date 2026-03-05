package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "帖子媒体")
public class PostMediaVO {

    @Schema(description = "媒体ID")
    private Long id;

    @Schema(description = "媒体地址")
    private String mediaUrl;

    @Schema(description = "媒体类型: 1图片/2视频")
    private Integer mediaType;

    @Schema(description = "排序权重")
    private Integer sortOrder;
}
