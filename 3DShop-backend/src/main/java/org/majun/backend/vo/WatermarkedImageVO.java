package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 水印图片响应VO
 */
@Data
@Schema(description = "水印图片响应")
public class WatermarkedImageVO {

    @Schema(description = "原图URL")
    private String originalUrl;

    @Schema(description = "水印图URL")
    private String watermarkedUrl;

    @Schema(description = "水印文字")
    private String watermarkText;

    @Schema(description = "图片ID")
    private Long imageId;

    @Schema(description = "模型ID")
    private Long modelId;
}
