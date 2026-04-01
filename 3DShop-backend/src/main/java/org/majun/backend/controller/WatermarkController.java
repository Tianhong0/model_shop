package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.majun.backend.annotation.OperationLog;
import org.majun.backend.common.Result;
import org.majun.backend.service.ImageWatermarkService;
import org.majun.backend.service.ModelService;
import org.majun.backend.vo.WatermarkStatusVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 水印管理控制器
 */
@Tag(name = "水印管理", description = "图片水印处理相关接口")
@RestController
@RequestMapping("/api/admin/watermark")
@RequiredArgsConstructor
public class WatermarkController {

    private final ImageWatermarkService imageWatermarkService;
    private final ModelService modelService;

    @Operation(summary = "为模型图片添加水印", description = "批量为指定模型的所有图片添加水印")
    @OperationLog(type = "UPDATE", module = "水印管理", description = "生成模型水印", targetType = "MODEL")
    @PostMapping("/generate/{modelId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<Map<String, Object>> generateWatermark(@PathVariable Long modelId) {
        int count = imageWatermarkService.batchAddWatermark(modelId);
        Map<String, Object> result = new HashMap<>();
        result.put("modelId", modelId);
        result.put("processedCount", count);
        return Result.success(result);
    }

    @Operation(summary = "检查模型水印状态", description = "检查模型是否已生成水印，返回详细状态信息")
    @GetMapping("/status/{modelId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<WatermarkStatusVO> checkWatermarkStatus(@PathVariable Long modelId) {
        WatermarkStatusVO status = modelService.getWatermarkStatus(modelId);
        return Result.success(status);
    }

    @Operation(summary = "重新生成水印", description = "强制重新生成水印，使用最新的水印配置")
    @OperationLog(type = "UPDATE", module = "水印管理", description = "重新生成水印", targetType = "MODEL")
    @PostMapping("/regenerate/{modelId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<Map<String, Object>> regenerateWatermark(@PathVariable Long modelId) {
        // 强制重新生成水印（会替换现有的水印）
        int count = imageWatermarkService.batchRegenerateWatermark(modelId);
        Map<String, Object> result = new HashMap<>();
        result.put("modelId", modelId);
        result.put("processedCount", count);
        return Result.success(result);
    }

    @Operation(summary = "批量生成水印", description = "批量为多个模型生成水印")
    @OperationLog(type = "UPDATE", module = "水印管理", description = "批量生成水印", targetType = "MODEL")
    @PostMapping("/batch-generate")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<Map<String, Object>> batchGenerateWatermark(@RequestBody BatchWatermarkRequest request) {
        int totalCount = 0;
        int successCount = 0;

        for (Long modelId : request.getModelIds()) {
            try {
                int count = imageWatermarkService.batchAddWatermark(modelId);
                totalCount += count;
                successCount++;
            } catch (Exception e) {
                // 单个模型失败不影响其他
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalProcessedImages", totalCount);
        result.put("successModels", successCount);
        result.put("totalModels", request.getModelIds().size());
        return Result.success(result);
    }

    @Operation(summary = "删除模型水印", description = "删除指定模型的所有水印记录")
    @OperationLog(type = "DELETE", module = "水印管理", description = "删除水印", targetType = "MODEL")
    @DeleteMapping("/delete/{modelId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<Void> deleteWatermark(@PathVariable Long modelId) {
        imageWatermarkService.deleteWatermarks(modelId);
        return Result.success();
    }

    /**
     * 批量生成水印请求
     */
    @lombok.Data
    public static class BatchWatermarkRequest {
        private List<Long> modelIds;
    }
}
