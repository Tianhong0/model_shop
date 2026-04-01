package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 水印状态VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "水印状态响应")
public class WatermarkStatusVO {

    /**
     * 模型ID
     */
    @Schema(description = "模型ID")
    private Long modelId;

    /**
     * 模型名称
     */
    @Schema(description = "模型名称")
    private String modelName;

    /**
     * 总图片数
     */
    @Schema(description = "总图片数")
    private Integer totalImages;

    /**
     * 已生成水印数
     */
    @Schema(description = "已生成水印数")
    private Integer watermarkedImages;

    /**
     * 水印覆盖率（百分比）
     */
    @Schema(description = "水印覆盖率（百分比）")
    private Integer coveragePercent;

    /**
     * 是否全部生成
     */
    @Schema(description = "是否全部生成")
    private Boolean isComplete;
}
