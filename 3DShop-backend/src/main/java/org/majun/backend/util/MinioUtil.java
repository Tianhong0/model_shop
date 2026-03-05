package org.majun.backend.util;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;

@Component
public class MinioUtil {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    /**
     * 上传文件
     * @param file SpringMVC 接收到的 MultipartFile
     * @param folder 业务分类目录，如 "avatars" 或 "models"
     * @return 文件的完整访问 URL
     */
    public String uploadFile(MultipartFile file, String folder) throws Exception {
        // 1. 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String suffix = resolveSuffix(originalFilename, file.getContentType());
        String fileName = folder + "/" + UUID.randomUUID() + suffix;
        String contentType = resolveContentType(file.getContentType(), suffix);

        // 2. 获取输入流
        try (InputStream inputStream = file.getInputStream()) {
            // 3. 执行上传 (显式传入 size, 解决 501 Not Implemented 错误)
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );
        }

        // 4. 返回拼接后的访问路径
        return endpoint + "/" + bucketName + "/" + fileName;
    }

    private String resolveSuffix(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        }
        if (contentType == null) {
            return ".bin";
        }
        String lower = contentType.toLowerCase(Locale.ROOT);
        if (lower.contains("jpeg")) return ".jpg";
        if (lower.contains("png")) return ".png";
        if (lower.contains("webp")) return ".webp";
        if (lower.contains("gif")) return ".gif";
        if (lower.contains("mp4")) return ".mp4";
        if (lower.contains("quicktime") || lower.contains("mov")) return ".mov";
        if (lower.contains("webm")) return ".webm";
        if (lower.contains("ogg")) return ".ogg";
        return ".bin";
    }

    private String resolveContentType(String contentType, String suffix) {
        if (contentType != null && !contentType.isBlank() && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            return contentType;
        }
        String lowerSuffix = suffix == null ? "" : suffix.toLowerCase(Locale.ROOT);
        return switch (lowerSuffix) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".webp" -> "image/webp";
            case ".gif" -> "image/gif";
            case ".mp4" -> "video/mp4";
            case ".mov" -> "video/quicktime";
            case ".webm" -> "video/webm";
            case ".ogg" -> "video/ogg";
            default -> "application/octet-stream";
        };
    }
}