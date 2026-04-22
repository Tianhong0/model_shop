package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.Result;
import org.majun.backend.dto.PromotionConfigUpdateRequest;
import org.majun.backend.service.PromotionService;
import org.majun.backend.vo.PosterConfigVO;
import org.majun.backend.vo.PromotionConfigVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 推广配置管理接口（管理端）
 */
@Tag(name = "Promotion Admin", description = "推广管理端接口")
@RestController
@RequestMapping("/api/promotion/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class PromotionAdminController {

    private final PromotionService promotionService;

    @Operation(summary = "获取所有推广配置")
    @GetMapping("/configs")
    public Result<List<PromotionConfigVO>> getAllConfigs() {
        return Result.success(promotionService.getAllConfigs());
    }

    @Operation(summary = "更新推广配置")
    @PostMapping("/config/update")
    public Result<Void> updateConfig(@RequestBody @Valid PromotionConfigUpdateRequest request) {
        promotionService.updateConfig(request.getConfigKey(), request.getConfigValue(), request.getConfigDesc());
        return Result.success();
    }

    @Operation(summary = "批量更新推广配置")
    @PostMapping("/configs/batch")
    public Result<Void> batchUpdateConfigs(@RequestBody List<PromotionConfigUpdateRequest> requests) {
        for (PromotionConfigUpdateRequest request : requests) {
            promotionService.updateConfig(request.getConfigKey(), request.getConfigValue(), request.getConfigDesc());
        }
        return Result.success();
    }
}
