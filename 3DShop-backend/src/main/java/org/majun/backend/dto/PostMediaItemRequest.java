package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "帖子媒体项")
public class PostMediaItemRequest {

    @NotBlank(message = "媒体地址不能为空")
    @Schema(description = "媒体地址")
    private String mediaUrl;

    @NotNull(message = "媒体类型不能为空")
    @Schema(description = "媒体类型: 1-图片, 2-视频")
    private Integer mediaType;

    @Schema(description = "排序权重")
    private Integer sortOrder = 0;
}
