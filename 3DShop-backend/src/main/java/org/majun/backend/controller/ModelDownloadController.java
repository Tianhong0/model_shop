package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.Result;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.ModelDownloadService;
import org.majun.backend.vo.ModelDownloadUrlVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模型下载控制器
 */
@Tag(name = "模型下载", description = "模型下载相关接口")
@RestController
@RequestMapping("/api/model/download")
@RequiredArgsConstructor
public class ModelDownloadController {

    private final ModelDownloadService modelDownloadService;

    @Operation(summary = "获取模型下载链接", description = "生成带过期时间的模型下载链接，需要购买权限")
    @GetMapping("/url/{modelId}")
    @PreAuthorize("isAuthenticated()")
    public Result<ModelDownloadUrlVO> getDownloadUrl(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long modelId,
            HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        ModelDownloadUrlVO vo = modelDownloadService.generateDownloadUrl(
                modelId,
                loginUser.getUser().getId(),
                ipAddress
        );
        return Result.success(vo);
    }

    @Operation(summary = "获取预览模型链接", description = "获取模型预览文件链接，无需购买权限")
    @GetMapping("/preview/{modelId}")
    public Result<String> getPreviewUrl(@PathVariable Long modelId) {
        String previewUrl = modelDownloadService.generatePreviewUrl(modelId);
        return Result.success(previewUrl);
    }

    @Operation(summary = "检查下载权限", description = "检查当前用户是否有权下载指定模型")
    @GetMapping("/can-download/{modelId}")
    @PreAuthorize("isAuthenticated()")
    public Result<Boolean> canDownload(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long modelId) {
        boolean canDownload = modelDownloadService.canDownloadModel(
                modelId,
                loginUser.getUser().getId()
        );
        return Result.success(canDownload);
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
