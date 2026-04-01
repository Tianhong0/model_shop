package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 模型详情响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "模型详情响应")
public class ModelDetailVO {

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
     * 模型描述
     */
    @Schema(description = "模型描述")
    private String description;


    /**
     * 授权说明
     */
    @Schema(description = "授权说明")
    private String licenseType;


    /**
     * 分类ID
     */
    @Schema(description = "分类ID")
    private Long categoryId;

    /**
     * 分类名称
     */
    @Schema(description = "分类名称")
    private String categoryName;

    /**
     * 设计者ID
     */
    @Schema(description = "设计者ID")
    private Long designerId;

    /**
     * 设计者昵称
     */
    @Schema(description = "设计者昵称")
    private String designerName;

    /**
     * 基础价格
     */
    @Schema(description = "基础价格")
    private BigDecimal basePrice;

    /**
     * 原始体积 (mm³)
     */
    @Schema(description = "原始体积(mm³)")
    private BigDecimal baseVolume;

    /**
     * 原始三维尺寸
     */
    @Schema(description = "原始三维尺寸(L*W*H)")
    private String baseSize;

    /**
     * 主图URL
     */
    @Schema(description = "主图URL")
    private String mainImageUrl;

    /**
     * 模型文件路径
     */
    @Schema(description = "模型文件路径")
    private String filePath;

    /**
     * 是否已购买
     */
    @Schema(description = "是否已购买")
    private Boolean purchased;

    /**
     * 预览URL（带水印/低精度）
     */
    @Schema(description = "预览URL")
    private String previewUrl;

    /**
     * 文件大小(字节)
     */
    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    /**
     * 下载次数
     */
    @Schema(description = "下载次数")
    private Integer downloadCount;

    /**
     * 上架状态: 0-审核中, 1-上架, 2-下架
     */
    @Schema(description = "上架状态: 0-审核中, 1-上架, 2-下架")
    private Integer status;

    /**
     * 模型图片列表
     */
    @Schema(description = "模型图片列表")
    private List<ModelImageVO> images;

    /**
     * 可用材质列表
     */
    @Schema(description = "可用材质列表")
    private List<MaterialVO> materials;
}
