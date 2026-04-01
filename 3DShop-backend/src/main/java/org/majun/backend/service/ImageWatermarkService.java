package org.majun.backend.service;

import org.majun.backend.vo.WatermarkedImageVO;

/**
 * 图片水印处理服务接口
 */
public interface ImageWatermarkService {

    /**
     * 为图片添加水印
     * @param originalImageUrl 原图URL
     * @param watermarkText 水印文字（为空则使用默认水印）
     * @return 水印后的图片URL
     */
    String addWatermark(String originalImageUrl, String watermarkText);

    /**
     * 为图片添加水印（使用默认水印文字）
     * @param originalImageUrl 原图URL
     * @return 水印后的图片URL
     */
    String addWatermark(String originalImageUrl);

    /**
     * 为模型图片添加水印（如果尚未添加）
     * @param modelId 模型ID
     * @param imageId 图片ID
     * @param originalImageUrl 原图URL
     * @return 水印后的图片URL
     */
    String getOrAddWatermark(Long modelId, Long imageId, String originalImageUrl);

    /**
     * 批量为模型图片添加水印
     * @param modelId 模型ID
     * @return 处理的图片数量
     */
    int batchAddWatermark(Long modelId);

    /**
     * 强制重新生成模型水印（会替换现有的水印）
     * @param modelId 模型ID
     * @return 处理的图片数量
     */
    int batchRegenerateWatermark(Long modelId);

    /**
     * 获取模型的水印预览图URL
     * @param modelId 模型ID
     * @param imageId 图片ID
     * @return 水印图URL，不存在返回null
     */
    String getWatermarkedUrl(Long modelId, Long imageId);

    /**
     * 检查图片是否已添加水印
     * @param modelId 模型ID
     * @param imageId 图片ID
     * @return 是否已添加
     */
    boolean hasWatermark(Long modelId, Long imageId);

    /**
     * 删除模型的水印记录（用于重新生成）
     * @param modelId 模型ID
     */
    void deleteWatermarks(Long modelId);
}
