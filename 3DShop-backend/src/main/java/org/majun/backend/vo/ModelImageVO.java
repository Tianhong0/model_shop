package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型图片VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "模型图片")
public class ModelImageVO {

    /**
     * 图片ID
     */
    @Schema(description = "图片ID")
    private Long id;

    /**
     * 图片URL
     */
    @Schema(description = "图片URL")
    private String imageUrl;

    /**
     * 水印图片URL（预览用）
     */
    @Schema(description = "水印图片URL")
    private String watermarkedUrl;

    /**
     * 是否为主图: 1-是, 0-否
     */
    @Schema(description = "是否为主图")
    private Integer isMain;

    /**
     * 图片类型: 1-3D渲染图, 2-实物效果图, 3-参数图
     */
    @Schema(description = "图片类型: 1-3D渲染图, 2-实物效果图, 3-参数图")
    private Integer imgType;

    /**
     * 排序权重
     */
    @Schema(description = "排序权重")
    private Integer sortOrder;
}
