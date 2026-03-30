package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.Result;
import org.majun.backend.common.ResultCode;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.util.MinioUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

@Slf4j
@Tag(name = "Community File", description = "社区媒体上传")
@RestController
@RequestMapping("/api/community/file")
@RequiredArgsConstructor
public class CommunityFileController {

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024L;
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024L;
    private static final long MAX_MODEL_SIZE = 50 * 1024 * 1024L;

    private final MinioUtil minioUtil;

    @Operation(summary = "上传社区媒体", description = "type: postImg/postVideo/model")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_DESIGNER') or hasAuthority('ROLE_ADMIN')")
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam("type") String type) {
        validateUpload(file, type);
        try {
            String folder = switch (type) {
                case "postImg" -> "community/images";
                case "postVideo" -> "community/videos";
                case "model" -> "event/models";
                default -> "community/others";
            };
            String url = minioUtil.uploadFile(file, folder);

            if ("postVideo".equals(type)) {
                try {
                    generateCoverFromMinIO(url);
                } catch (Exception e) {
                    log.warn("视频封面生成失败: {}", e.getMessage());
                }
            }

            return Result.success(url);
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.FAIL, "上传失败: " + ex.getMessage());
        }
    }

    /**
     * 从 MinIO 下载视频并用 FFmpeg 截取封面，上传封面到 MinIO。
     * 可用于新上传的视频和批量补生成旧视频封面。
     */
    public void generateCoverFromMinIO(String videoUrl) throws Exception {
        String objectName = minioUtil.extractObjectName(videoUrl);
        if (objectName == null) return;
        String coverObjectName = objectName.replaceAll("\\.[^.]+$", "-cover.jpg");

        // 已有封面则跳过
        if (minioUtil.objectExists(coverObjectName)) return;

        File tempVideo = File.createTempFile("video-", ".tmp");
        File tempCover = File.createTempFile("cover-", ".jpg");
        try {
            // 从 MinIO 下载视频（不依赖 MultipartFile）
            minioUtil.downloadFile(objectName, tempVideo);

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-i", tempVideo.getAbsolutePath(),
                    "-ss", "00:00:00.500", "-vframes", "1",
                    "-vf", "scale='min(720,iw)':-2",
                    "-q:v", "5",
                    "-f", "image2", "-y", tempCover.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.getInputStream().readAllBytes();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("FFmpeg 超时: {}", objectName);
                return;
            }
            if (process.exitValue() != 0 || tempCover.length() == 0) {
                log.warn("FFmpeg 失败(exit={}): {}", process.exitValue(), objectName);
                return;
            }

            byte[] coverBytes = Files.readAllBytes(tempCover.toPath());
            minioUtil.uploadBytes(coverBytes, "image/jpeg", coverObjectName);
            log.info("封面生成成功: {}", coverObjectName);
        } finally {
            tempVideo.delete();
            tempCover.delete();
        }
    }

    private void validateUpload(MultipartFile file, String type) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件不能为空");
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();

        if ("postImg".equals(type)) {
            if (!contentType.startsWith("image/")) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "仅支持图片文件");
            }
            if (file.getSize() > MAX_IMAGE_SIZE) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "图片大小不能超过10MB");
            }
            return;
        }

        if ("postVideo".equals(type)) {
            if (!contentType.startsWith("video/")) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "仅支持视频文件");
            }
            if (file.getSize() > MAX_VIDEO_SIZE) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "视频大小不能超过100MB");
            }
            return;
        }

        if ("model".equals(type)) {
            boolean validExtension = filename.endsWith(".stl") || filename.endsWith(".obj") ||
                    filename.endsWith(".3mf") || filename.endsWith(".ply") ||
                    filename.endsWith(".step") || filename.endsWith(".stp");
            if (!validExtension) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "仅支持STL、OBJ、3MF、PLY、STEP格式的模型文件");
            }
            if (file.getSize() > MAX_MODEL_SIZE) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "模型文件大小不能超过50MB");
            }
            return;
        }

        throw new BusinessException(ResultCode.PARAM_ERROR, "type仅支持postImg、postVideo或model");
    }
}
