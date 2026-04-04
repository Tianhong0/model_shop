package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.majun.backend.ai.service.SemanticSearchService;
import org.majun.backend.ai.service.SemanticSearchService.IndexStatus;
import org.majun.backend.common.Result;
import org.majun.backend.vo.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 语义搜索控制器
 * 提供基于 AI 向量的语义搜索功能
 */
@Tag(name = "语义搜索", description = "基于 AI 向量的语义搜索接口")
@RestController
@RequestMapping("/api/semantic-search")
@RequiredArgsConstructor
public class SemanticSearchController {

    private final SemanticSearchService semanticSearchService;

    /**
     * 语义搜索模型
     */
    @Operation(summary = "语义搜索模型", description = "使用自然语言描述搜索相关模型，支持语义理解")
    @GetMapping("/models")
    public Result<PageResult<?>> searchModels(
            @Parameter(description = "搜索查询文本") @RequestParam String query,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {

        if (query == null || query.trim().isEmpty()) {
            return Result.success(PageResult.<Object>builder()
                    .records(java.util.Collections.emptyList())
                    .total(0L)
                    .pageNum(pageNum)
                    .pageSize(pageSize)
                    .pages(0)
                    .build());
        }

        PageResult<?> result = semanticSearchService.searchModels(query.trim(), pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取索引状态
     */
    @Operation(summary = "获取索引状态", description = "查看语义搜索索引的状态信息")
    @GetMapping("/status")
    public Result<IndexStatus> getIndexStatus() {
        return Result.success(semanticSearchService.getIndexStatus());
    }

    /**
     * 重建索引（管理员）
     */
    @Operation(summary = "重建索引", description = "重新索引所有模型数据，管理员权限")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/reindex")
    public Result<String> reindex() {
        semanticSearchService.indexAllModels();
        return Result.success("索引重建任务已启动");
    }
}
