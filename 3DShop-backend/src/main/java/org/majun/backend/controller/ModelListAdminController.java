package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.Result;
import org.majun.backend.dto.ModelListAdminQueryRequest;
import org.majun.backend.dto.ModelListBatchRequest;
import org.majun.backend.dto.ModelListStatusUpdateRequest;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.ModelListService;
import org.majun.backend.vo.ModelListAdminVO;
import org.majun.backend.vo.ModelListStatisticsVO;
import org.majun.backend.vo.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ModelList Admin", description = "模型清单管理端接口")
@RestController
@RequestMapping("/api/model-list/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class ModelListAdminController {

    private final ModelListService modelListService;

    @Operation(summary = "清单管理分页查询")
    @PostMapping("/page")
    public Result<PageResult<ModelListAdminVO>> getAdminPage(@Valid @RequestBody ModelListAdminQueryRequest request) {
        return Result.success(modelListService.getAdminPage(request));
    }

    @Operation(summary = "清单管理详情")
    @GetMapping("/detail/{listId}")
    public Result<ModelListAdminVO> getAdminDetail(@PathVariable Long listId) {
        return Result.success(modelListService.getAdminDetail(listId));
    }

    @Operation(summary = "更新清单状态")
    @PostMapping("/status")
    public Result<Void> updateStatus(@Valid @RequestBody ModelListStatusUpdateRequest request) {
        modelListService.updateStatus(request);
        return Result.success();
    }

    @Operation(summary = "删除清单")
    @PostMapping("/delete/{listId}")
    public Result<Void> adminDeleteList(@PathVariable Long listId) {
        modelListService.adminDeleteList(listId);
        return Result.success();
    }

    @Operation(summary = "批量更新清单状态")
    @PostMapping("/batch/status")
    public Result<Void> batchUpdateStatus(@Valid @RequestBody ModelListBatchRequest request) {
        modelListService.batchUpdateStatus(request);
        return Result.success();
    }

    @Operation(summary = "批量删除清单")
    @PostMapping("/batch/delete")
    public Result<Void> batchDelete(@Valid @RequestBody ModelListBatchRequest request) {
        modelListService.batchDelete(request);
        return Result.success();
    }

    @Operation(summary = "清单统计数据")
    @GetMapping("/statistics")
    public Result<ModelListStatisticsVO> getStatistics() {
        return Result.success(modelListService.getStatistics());
    }
}
