package org.majun.backend.util;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;

@Component
public class MinioUtil {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucketName;

    /** 内网地址 — 后端下载/上传用 */
    @Value("${minio.endpoint}")
    private String endpoint;

    /** 公网地址 — 拼接返回给前端的 URL */
    @Value("${minio.public-endpoint}")
    private String publicEndpoint;

    /**
     * 上传文件，返回公网可访问的 URL（给前端/客户端使用）
     */
    public String uploadFile(MultipartFile file, String folder) throws Exception {
        String url = uploadFileInternal(file, folder);
        // 转换为公网 URL 返回给前端
        return toPublicUrl(url);
    }

    /**
     * 内部上传，返回内网 URL（给后端切片服务使用）
     */
    public String uploadFileInternal(MultipartFile file, String folder) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String suffix = resolveSuffix(originalFilename, file.getContentType());
        String fileName = folder + "/" + UUID.randomUUID() + suffix;
        String contentType = resolveContentType(file.getContentType(), suffix);

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );
        }

        // 返回内网地址
        return endpoint + "/" + bucketName + "/" + fileName;
    }

    /**
     * 将内网 URL 转换为公网 URL，用于返回给前端
     */
    public String toPublicUrl(String url) {
        if (url == null || publicEndpoint == null || endpoint == null) {
            return url;
        }
        if (url.startsWith(endpoint)) {
            return publicEndpoint + url.substring(endpoint.length());
        }
        return url;
    }

    /**
     * 将公网 URL 转换为内网 URL，用于后端内部下载（如切片前下载模型）。
     * 如果 URL 不是以 publicEndpoint 开头，则原样返回。
     */
    public String toInternalUrl(String url) {
        if (url == null || publicEndpoint == null || endpoint == null) {
            return url;
        }
        if (url.startsWith(publicEndpoint)) {
            return endpoint + url.substring(publicEndpoint.length());
        }
        return url;
    }

    /**
     * 获取内网 endpoint（切片服务等内部调用使用）
     */
    public String getInternalEndpoint() {
        return endpoint;
    }

    /**
     * 获取公网 endpoint（返回给前端使用）
     */
    public String getPublicEndpoint() {
        return publicEndpoint;
    }

    /**
     * 上传字节数组到指定对象路径，返回公网 URL
     */
    public String uploadBytes(byte[] data, String contentType, String objectName) throws Exception {
        try (InputStream is = new ByteArrayInputStream(data)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(is, data.length, -1)
                            .contentType(contentType)
                            .build()
            );
        }
        return toPublicUrl(endpoint + "/" + bucketName + "/" + objectName);
    }

    /**
     * 从公网/内网 URL 提取 MinIO 对象名称
     */
    public String extractObjectName(String url) {
        if (url == null) return null;
        String prefix = "/" + bucketName + "/";
        int idx = url.indexOf(prefix);
        if (idx >= 0) {
            return url.substring(idx + prefix.length());
        }
        return null;
    }

    /**
     * 下载 MinIO 对象到本地文件
     */
    public void downloadFile(String objectName, File targetFile) throws Exception {
        try (InputStream is = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucketName).object(objectName).build());
             FileOutputStream fos = new FileOutputStream(targetFile)) {
            is.transferTo(fos);
        }
    }

    /**
     * 检查 MinIO 对象是否存在
     */
    public boolean objectExists(String objectName) {
        try {
            minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectName).build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
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
