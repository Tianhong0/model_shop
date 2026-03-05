package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 模型列表响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "模型列表响应")
public class ModelListVO {

    /**
     * 模型ID
     */
    @Schema(description = "模型ID")
    private Long id;

    /**
     * 模型名称
     */
    @Schema(description = "模型名称")
    private String modelName;

    /**
     * 主图URL
     */
    @Schema(description = "主图URL")
    private String mainImageUrl;

    /**
     * 设计者昵称
     */
    @Schema(description = "设计者昵称")
    private String designerName;

    /**
     * 基础价格
     */
    @Schema(description = "基础价格")
    private String basePrice;

    @Schema(description = "原始体积(mm³)")
    private String baseVolume;

    @Schema(description = "原始三维尺寸(L*W*H)")
    private String baseSize;

    /**
     * 模型分类名称
     */
    @Schema(description = "模型分类名称")
    private String categoryName;

    /**
     * 上架状态: 0-审核中, 1-上架, 2-下架
     */
    @Schema(description = "上架状态: 0-审核中, 1-上架, 2-下架")
    private Integer status;

    /**
     * 模型图片数量
     */
    @Schema(description = "模型图片数量")
    private Integer imageCount;
}
