package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.majun.backend.annotation.OperationLog;
import org.majun.backend.common.Result;
import org.majun.backend.dto.ConfigUpdateRequest;
import org.majun.backend.service.ConfigService;
import org.majun.backend.vo.ConfigVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统配置控制器
 */
@Tag(name = "系统配置", description = "系统配置管理接口")
@RestController
@RequestMapping("/api/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    /** 获取配置值 */
    @Operation(summary = "获取配置值", description = "根据key获取配置值")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/{key}")
    public Result<ConfigVO> getConfig(@PathVariable String key) {
        ConfigVO config = configService.getConfigDetail(key);
        return config != null ? Result.success(config) : Result.fail("配置不存在");
    }

    /** 获取所有配置 */
    @Operation(summary = "获取所有配置", description = "管理员获取所有系统配置")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping
    public Result<List<ConfigVO>> getAllConfigs() {
        return Result.success(configService.getAllConfigs());
    }

    /** 获取分组配置 */
    @Operation(summary = "获取分组配置", description = "获取指定分组的配置列表")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/group/{group}")
    public Result<List<ConfigVO>> getConfigsByGroup(@PathVariable String group) {
        return Result.success(configService.getConfigsByGroup(group));
    }

    /** 获取公开配置 */
    @Operation(summary = "获取公开配置", description = "获取公开配置，无需权限")
    @GetMapping("/public")
    public Result<Map<String, String>> getPublicConfigs() {
        return Result.success(configService.getPublicConfigs());
    }

    /** 设置配置 */
    @Operation(summary = "设置配置", description = "管理员设置系统配置")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @OperationLog(type = "UPDATE", module = "系统配置", description = "修改系统配置")
    @PostMapping
    public Result<Void> setConfig(@Valid @RequestBody ConfigUpdateRequest request) {
        configService.setConfig(request);
        return Result.success();
    }

    /** 批量设置配置 */
    @Operation(summary = "批量设置配置", description = "管理员批量设置系统配置")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @OperationLog(type = "UPDATE", module = "系统配置", description = "批量修改系统配置")
    @PostMapping("/batch")
    public Result<Void> setConfigs(@RequestBody Map<String, String> configs) {
        configService.setConfigs(configs);
        return Result.success();
    }

    /** 删除配置 */
    @Operation(summary = "删除配置", description = "管理员删除系统配置")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @OperationLog(type = "DELETE", module = "系统配置", description = "删除系统配置")
    @DeleteMapping("/{key}")
    public Result<Void> deleteConfig(@PathVariable String key) {
        configService.deleteConfig(key);
        return Result.success();
    }
}
