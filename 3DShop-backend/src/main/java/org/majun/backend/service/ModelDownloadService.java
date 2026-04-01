package org.majun.backend.service;

import org.majun.backend.vo.ModelDownloadUrlVO;

/**
 * 模型下载服务接口
 */
public interface ModelDownloadService {

    /**
     * 验证用户是否有权下载模型
     * @param modelId 模型ID
     * @param userId 用户ID
     * @return 验证结果
     */
    boolean canDownloadModel(Long modelId, Long userId);

    /**
     * 生成模型下载的Presigned URL
     * @param modelId 模型ID
     * @param userId 用户ID
     * @param ipAddress 请求IP
     * @return 包含下载URL和过期时间的结果
     */
    ModelDownloadUrlVO generateDownloadUrl(Long modelId, Long userId, String ipAddress);

    /**
     * 获取预览模型的Presigned URL（无需购买验证）
     * @param modelId 模型ID
     * @return 预览URL
     */
    String generatePreviewUrl(Long modelId);

    /**
     * 获取图片的水印URL
     * @param modelId 模型ID
     * @param imageId 图片ID
     * @return 水印图URL，不存在返回null
     */
    String getWatermarkedImageUrl(Long modelId, Long imageId);
}
