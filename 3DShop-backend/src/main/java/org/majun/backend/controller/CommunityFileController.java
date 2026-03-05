package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

@Tag(name = "Community File", description = "社区媒体上传")
@RestController
@RequestMapping("/api/community/file")
@RequiredArgsConstructor
public class CommunityFileController {

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024L;
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024L;

    private final MinioUtil minioUtil;

    @Operation(summary = "上传社区媒体", description = "type: postImg/postVideo")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_DESIGNER') or hasAuthority('ROLE_ADMIN')")
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam("type") String type) {
        validateUpload(file, type);
        try {
            String folder = "postImg".equals(type) ? "community/images" : "community/videos";
            return Result.success(minioUtil.uploadFile(file, folder));
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.FAIL, "上传失败: " + ex.getMessage());
        }
    }

    private void validateUpload(MultipartFile file, String type) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件不能为空");
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
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

        throw new BusinessException(ResultCode.PARAM_ERROR, "type仅支持postImg或postVideo");
    }
}
