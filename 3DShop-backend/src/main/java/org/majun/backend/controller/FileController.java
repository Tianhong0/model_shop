package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.majun.backend.common.Result;

import org.majun.backend.util.MinioUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/file")
/**
 * 文件上传控制器
 */
public class FileController {

    @Autowired
    private MinioUtil minioUtil; // 使用我们之前提到的工具类

    /**
     * 上传 3D 模型预览图或头像
     */
    @Operation(summary = "上传文件", description = "登录用户可上传头像，管理员和设计者可上传其它资源")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_DESIGNER') or hasAuthority('ROLE_ADMIN')")
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam("type") String type) {
        try {
            // 根据类型决定存放在哪个文件夹，如 "avatars" 或 "models"
            String folder = switch (type) {
                case "avatar"    -> "avatars";
                case "modelImg"  -> "model-images";
                case "banner"    -> "banners";
                case "modelFile" -> "models";
                default          -> "others";
            };
            String url = minioUtil.uploadFile(file, folder);
            return Result.success(url);
        } catch (Exception e) {
            return Result.fail("上传失败：" + e.getMessage());
        }
    }
}