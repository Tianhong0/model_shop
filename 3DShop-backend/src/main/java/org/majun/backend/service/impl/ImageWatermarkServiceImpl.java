package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.entity.ModelImageWatermark;
import org.majun.backend.entity.SysModelImage;
import org.majun.backend.repository.ModelImageWatermarkRepository;
import org.majun.backend.repository.SysModelImageRepository;
import org.majun.backend.service.ConfigService;
import org.majun.backend.service.ImageWatermarkService;
import org.majun.backend.util.MinioUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 图片水印处理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageWatermarkServiceImpl implements ImageWatermarkService {

    private static final String DEFAULT_WATERMARK_TEXT = "3D打印定制商城";
    private static final String WATERMARK_CONFIG_KEY = "WATERMARK_TEXT";
    private static final String WATERMARK_POSITION_KEY = "WATERMARK_POSITION"; // CENTER, BOTTOM_RIGHT, TILE
    private static final float DEFAULT_ALPHA = 0.3f; // 水印透明度

    private final ModelImageWatermarkRepository watermarkRepository;
    private final SysModelImageRepository modelImageRepository;
    private final MinioUtil minioUtil;
    private final ConfigService configService;

    @Override
    public String addWatermark(String originalImageUrl) {
        return addWatermark(originalImageUrl, null);
    }

    @Override
    public String addWatermark(String originalImageUrl, String watermarkText) {
        if (!StringUtils.hasText(originalImageUrl)) {
            log.warn("原图URL为空，跳过水印生成");
            return null;
        }

        String text = StringUtils.hasText(watermarkText) ? watermarkText : getWatermarkText();
        log.info("开始生成水印: originalUrl={}, watermarkText={}", originalImageUrl, text);

        try {
            // 下载原图
            BufferedImage originalImage = downloadImage(originalImageUrl);
            if (originalImage == null) {
                log.warn("无法下载原图，跳过水印生成: {}", originalImageUrl);
                return null;
            }

            log.info("原图下载成功，尺寸: {}x{}", originalImage.getWidth(), originalImage.getHeight());

            // 添加水印
            BufferedImage watermarkedImage = applyWatermark(originalImage, text);

            // 上传水印图
            String watermarkedUrl = uploadWatermarkedImage(watermarkedImage, originalImageUrl);
            if (watermarkedUrl == null) {
                log.error("上传水印图片失败: {}", originalImageUrl);
                return null;
            }

            log.info("水印添加成功: original={}, watermarked={}", originalImageUrl, watermarkedUrl);
            return watermarkedUrl;
        } catch (Exception e) {
            log.error("添加水印失败: originalUrl={}, error={}", originalImageUrl, e.getMessage(), e);
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String getOrAddWatermark(Long modelId, Long imageId, String originalImageUrl) {
        String currentWatermarkText = getWatermarkText();
        log.info("getOrAddWatermark: modelId={}, imageId={}, watermarkText={}", modelId, imageId, currentWatermarkText);

        // 先检查是否已有水印（包括软删除的记录）
        ModelImageWatermark existingWatermark = watermarkRepository.selectOne(
                new LambdaQueryWrapper<ModelImageWatermark>()
                        .eq(ModelImageWatermark::getModelId, modelId)
                        .eq(ModelImageWatermark::getOriginalImageId, imageId)
                        .last("LIMIT 1")
        );

        if (existingWatermark != null) {
            log.info("找到现有水印记录: id={}, isDelete={}, watermarkText={}, watermarkedUrl={}",
                    existingWatermark.getId(), existingWatermark.getIsDelete(),
                    existingWatermark.getWatermarkText(), existingWatermark.getWatermarkedUrl());
        }

        // 如果存在有效的水印记录，且水印文字与当前配置一致，直接返回
        if (existingWatermark != null && existingWatermark.getIsDelete() == 0
                && currentWatermarkText.equals(existingWatermark.getWatermarkText())) {
            log.info("水印配置未变更，返回现有水印URL: {}", existingWatermark.getWatermarkedUrl());
            return existingWatermark.getWatermarkedUrl();
        }

        // 水印文字配置已变更或不存在记录，需要重新生成
        log.info("需要重新生成水印: existingWatermark={}, textChanged={}",
                existingWatermark != null,
                existingWatermark != null && !currentWatermarkText.equals(existingWatermark.getWatermarkText()));

        // 添加水印
        String watermarkedUrl = addWatermark(originalImageUrl, currentWatermarkText);
        if (!StringUtils.hasText(watermarkedUrl)) {
            log.warn("水印生成失败，返回原图URL: {}", originalImageUrl);
            return originalImageUrl; // 失败时返回原图
        }

        log.info("水印生成成功，新URL: {}", watermarkedUrl);

        if (existingWatermark != null) {
            // 更新现有记录
            log.info("更新现有水印记录: id={}", existingWatermark.getId());
            existingWatermark.setOriginalUrl(originalImageUrl);
            existingWatermark.setWatermarkedUrl(watermarkedUrl);
            existingWatermark.setWatermarkText(currentWatermarkText);
            existingWatermark.setIsDelete(0);
            int updateResult = watermarkRepository.updateById(existingWatermark);
            log.info("更新结果: {}", updateResult);
        } else {
            // 不存在记录，创建新的
            log.info("创建新水印记录");
            ModelImageWatermark watermark = new ModelImageWatermark();
            watermark.setModelId(modelId);
            watermark.setOriginalImageId(imageId);
            watermark.setOriginalUrl(originalImageUrl);
            watermark.setWatermarkedUrl(watermarkedUrl);
            watermark.setWatermarkText(currentWatermarkText);
            int insertResult = watermarkRepository.insert(watermark);
            log.info("插入结果: {}, 新记录ID: {}", insertResult, watermark.getId());
        }

        return watermarkedUrl;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAddWatermark(Long modelId) {
        if (modelId == null) {
            return 0;
        }

        // 查询模型的所有图片（SysModelImage没有逻辑删除字段）
        List<SysModelImage> images = modelImageRepository.selectList(
                new LambdaQueryWrapper<SysModelImage>()
                        .eq(SysModelImage::getModelId, modelId)
        );

        if (images.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (SysModelImage image : images) {
            try {
                String watermarkedUrl = getOrAddWatermark(modelId, image.getId(), image.getImageUrl());
                if (StringUtils.hasText(watermarkedUrl)) {
                    count++;
                }
            } catch (Exception e) {
                log.error("为图片添加水印失败: imageId={}", image.getId(), e);
            }
        }

        log.info("批量添加水印完成: modelId={}, count={}", modelId, count);
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchRegenerateWatermark(Long modelId) {
        if (modelId == null) {
            return 0;
        }

        // 查询模型的所有图片
        List<SysModelImage> images = modelImageRepository.selectList(
                new LambdaQueryWrapper<SysModelImage>()
                        .eq(SysModelImage::getModelId, modelId)
        );

        if (images.isEmpty()) {
            return 0;
        }

        // 获取当前水印配置
        String watermarkText = getWatermarkText();
        int count = 0;

        for (SysModelImage image : images) {
            try {
                // 强制重新生成水印
                String watermarkedUrl = forceRegenerateWatermark(modelId, image.getId(), image.getImageUrl(), watermarkText);
                if (StringUtils.hasText(watermarkedUrl)) {
                    count++;
                }
            } catch (Exception e) {
                log.error("重新生成水印失败: imageId={}", image.getId(), e);
            }
        }

        log.info("批量重新生成水印完成: modelId={}, count={}", modelId, count);
        return count;
    }

    /**
     * 强制重新生成单个图片的水印
     */
    private String forceRegenerateWatermark(Long modelId, Long imageId, String originalImageUrl, String watermarkText) {
        log.info("强制重新生成水印: modelId={}, imageId={}, watermarkText={}", modelId, imageId, watermarkText);

        // 生成新的水印图片
        String watermarkedUrl = addWatermark(originalImageUrl, watermarkText);
        if (!StringUtils.hasText(watermarkedUrl)) {
            log.warn("生成水印图片失败: imageId={}", imageId);
            return originalImageUrl;
        }
        log.info("新水印图片URL: {}", watermarkedUrl);

        // 查找现有记录
        ModelImageWatermark existingWatermark = watermarkRepository.selectOne(
                new LambdaQueryWrapper<ModelImageWatermark>()
                        .eq(ModelImageWatermark::getModelId, modelId)
                        .eq(ModelImageWatermark::getOriginalImageId, imageId)
                        .last("LIMIT 1")
        );

        if (existingWatermark != null) {
            // 更新现有记录
            log.info("更新现有水印记录: id={}, oldUrl={}, newUrl={}", existingWatermark.getId(), existingWatermark.getWatermarkedUrl(), watermarkedUrl);
            existingWatermark.setOriginalUrl(originalImageUrl);
            existingWatermark.setWatermarkedUrl(watermarkedUrl);
            existingWatermark.setWatermarkText(watermarkText);
            existingWatermark.setIsDelete(0);
            watermarkRepository.updateById(existingWatermark);
        } else {
            // 创建新记录
            log.info("创建新水印记录");
            ModelImageWatermark watermark = new ModelImageWatermark();
            watermark.setModelId(modelId);
            watermark.setOriginalImageId(imageId);
            watermark.setOriginalUrl(originalImageUrl);
            watermark.setWatermarkedUrl(watermarkedUrl);
            watermark.setWatermarkText(watermarkText);
            watermarkRepository.insert(watermark);
        }

        return watermarkedUrl;
    }

    @Override
    public String getWatermarkedUrl(Long modelId, Long imageId) {
        ModelImageWatermark watermark = watermarkRepository.selectOne(
                new LambdaQueryWrapper<ModelImageWatermark>()
                        .eq(ModelImageWatermark::getModelId, modelId)
                        .eq(ModelImageWatermark::getOriginalImageId, imageId)
                        .eq(ModelImageWatermark::getIsDelete, 0)
        );
        if (watermark != null) {
            log.debug("获取水印URL: modelId={}, imageId={}, url={}", modelId, imageId, watermark.getWatermarkedUrl());
        }
        return watermark != null ? watermark.getWatermarkedUrl() : null;
    }

    @Override
    public boolean hasWatermark(Long modelId, Long imageId) {
        Long count = watermarkRepository.selectCount(
                new LambdaQueryWrapper<ModelImageWatermark>()
                        .eq(ModelImageWatermark::getModelId, modelId)
                        .eq(ModelImageWatermark::getOriginalImageId, imageId)
                        .eq(ModelImageWatermark::getIsDelete, 0)
        );
        return count != null && count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWatermarks(Long modelId) {
        // 使用物理删除，避免唯一索引冲突
        watermarkRepository.physicalDeleteByModelId(modelId);
        log.info("删除模型水印记录: modelId={}", modelId);
    }

    /**
     * 获取水印文字（从配置读取）
     */
    private String getWatermarkText() {
        String text = configService.getConfigValue(WATERMARK_CONFIG_KEY);
        return StringUtils.hasText(text) ? text : DEFAULT_WATERMARK_TEXT;
    }

    /**
     * 获取水印位置（从配置读取）
     */
    private String getWatermarkPosition() {
        String position = configService.getConfigValue(WATERMARK_POSITION_KEY);
        return StringUtils.hasText(position) ? position : "BOTTOM_RIGHT";
    }

    /**
     * 下载图片（优先使用MinIO客户端直接下载）
     */
    private BufferedImage downloadImage(String imageUrl) {
        try {
            // 提取MinIO对象名称
            String objectName = minioUtil.extractObjectName(imageUrl);
            log.debug("提取对象名称: url={}, objectName={}", imageUrl, objectName);

            if (StringUtils.hasText(objectName)) {
                // 使用MinIO客户端直接下载
                try (InputStream is = minioUtil.getObjectStream(objectName)) {
                    // 将流读取到字节数组，避免流只能读取一次的问题
                    byte[] imageBytes = is.readAllBytes();
                    log.debug("MinIO下载成功: objectName={}, size={}bytes", objectName, imageBytes.length);

                    // 尝试解析图片
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
                    if (image != null) {
                        log.debug("图片解析成功: {}x{}", image.getWidth(), image.getHeight());
                        return image;
                    } else {
                        // ImageIO不支持该格式，记录详细信息
                        log.warn("ImageIO无法解析图片格式: objectName={}, 可能需要额外插件支持(如WebP)", objectName);
                    }
                } catch (Exception e) {
                    log.warn("MinIO下载失败: objectName={}, error={}", objectName, e.getMessage());
                }
            } else {
                log.warn("无法从URL提取对象名称: {}", imageUrl);
            }

            // 回退到HTTP下载
            String downloadUrl = minioUtil.toInternalUrl(imageUrl);
            log.debug("尝试HTTP下载: {}", downloadUrl);
            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                log.warn("HTTP下载响应码: {}, url={}", responseCode, downloadUrl);
                return null;
            }

            try (InputStream is = connection.getInputStream()) {
                byte[] imageBytes = is.readAllBytes();
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
                if (image != null) {
                    log.debug("HTTP下载成功: {}", downloadUrl);
                }
                return image;
            }
        } catch (Exception e) {
            log.error("下载图片失败: url={}, error={}", imageUrl, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 为图片添加水印（斜放在中间）
     */
    private BufferedImage applyWatermark(BufferedImage originalImage, String watermarkText) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // 创建带Alpha通道的图片
        BufferedImage watermarkedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = watermarkedImage.createGraphics();

        // 绘制原图
        g2d.drawImage(originalImage, 0, 0, null);

        // 设置水印样式
        int fontSize = Math.max(width / 18, 20); // 字体大小根据图片宽度自适应
        g2d.setFont(new Font("微软雅黑", Font.BOLD, fontSize));
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 计算水印文字尺寸
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int textWidth = fontMetrics.stringWidth(watermarkText);
        int textHeight = fontMetrics.getHeight();

        // 将原点移动到图片中心
        int centerX = width / 2;
        int centerY = height / 2;
        g2d.translate(centerX, centerY);

        // 旋转 -30 度（逆时针）
        double angle = Math.toRadians(-30);
        g2d.rotate(angle);

        // 设置水印颜色（白色半透明）
        g2d.setColor(new Color(255, 255, 255, (int) (255 * DEFAULT_ALPHA)));

        // 绘制阴影效果
        g2d.setColor(new Color(0, 0, 0, (int) (255 * 0.15f)));
        g2d.drawString(watermarkText, -textWidth / 2 + 2, textHeight / 4 + 2);

        // 绘制水印文字（居中）
        g2d.setColor(new Color(255, 255, 255, (int) (255 * DEFAULT_ALPHA)));
        g2d.drawString(watermarkText, -textWidth / 2, textHeight / 4);

        g2d.dispose();
        return watermarkedImage;
    }

    /**
     * 上传水印图片到MinIO
     */
    private String uploadWatermarkedImage(BufferedImage watermarkedImage, String originalUrl) {
        try {
            // 转换为字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String format = getImageFormat(originalUrl);
            ImageIO.write(watermarkedImage, format, baos);
            byte[] bytes = baos.toByteArray();

            // 生成对象名称
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String objectName = "watermark/" + timestamp + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + format;

            // 上传
            return minioUtil.uploadBytes(bytes, "image/" + format, objectName);
        } catch (Exception e) {
            log.error("上传水印图片失败", e);
            return null;
        }
    }

    /**
     * 从URL获取图片格式
     */
    private String getImageFormat(String url) {
        if (url == null) return "png";
        String lower = url.toLowerCase();
        if (lower.contains(".jpg") || lower.contains(".jpeg")) return "jpg";
        if (lower.contains(".png")) return "png";
        if (lower.contains(".gif")) return "gif";
        if (lower.contains(".webp")) return "webp";
        return "png";
    }
}
