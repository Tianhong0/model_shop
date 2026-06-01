package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.Result;
import org.majun.backend.dto.*;
import org.majun.backend.entity.SysBatchPrintDiscount;
import org.majun.backend.service.GroupBuyService;
import org.majun.backend.vo.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端拼团活动接口
 */
@Tag(name = "GroupBuy Admin", description = "拼团活动管理API")
@RestController
@RequestMapping("/api/admin/group-buy")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class GroupBuyAdminController {

    private final GroupBuyService groupBuyService;

    /** 创建拼团活动 */
    @Operation(summary = "创建拼团活动")
    @PostMapping("/activity/create")
    public Result<Long> createActivity(@Valid @RequestBody GroupBuyActivityCreateRequest request) {
        return Result.success(groupBuyService.createActivity(request));
    }

    /** 更新拼团活动 */
    @Operation(summary = "更新拼团活动")
    @PutMapping("/activity/update")
    public Result<Void> updateActivity(@Valid @RequestBody GroupBuyActivityUpdateRequest request) {
        groupBuyService.updateActivity(request);
        return Result.success();
    }

    /** 分页查询拼团活动 */
    @Operation(summary = "分页查询拼团活动")
    @PostMapping("/activity/list")
    public Result<PageResult<GroupBuyActivityVO>> listActivities(@RequestBody(required = false) GroupBuyActivityQueryRequest request) {
        if (request == null) {
            request = new GroupBuyActivityQueryRequest();
        }
        return Result.success(groupBuyService.listActivities(request));
    }

    /** 获取活动详情 */
    @Operation(summary = "获取活动详情")
    @GetMapping("/activity/detail/{activityId}")
    public Result<GroupBuyActivityDetailVO> getActivityDetail(@PathVariable Long activityId) {
        return Result.success(groupBuyService.getActivityDetail(activityId));
    }

    /** 启用/禁用活动 */
    @Operation(summary = "启用/禁用活动")
    @PutMapping("/activity/status/{activityId}")
    public Result<Void> updateActivityStatus(
            @PathVariable Long activityId,
            @RequestParam Integer status) {
        groupBuyService.updateActivityStatus(activityId, status);
        return Result.success();
    }

    /** 获取批量打印折扣配置 */
    @Operation(summary = "获取批量打印折扣配置")
    @GetMapping("/batch-discount/list")
    public Result<List<SysBatchPrintDiscount>> getBatchDiscountList() {
        return Result.success(groupBuyService.getBatchDiscountList());
    }

    /** 保存批量打印折扣配置 */
    @Operation(summary = "保存批量打印折扣配置")
    @PostMapping("/batch-discount/save")
    public Result<Void> saveBatchDiscount(@RequestBody List<SysBatchPrintDiscount> configList) {
        groupBuyService.saveBatchDiscount(configList);
        return Result.success();
    }
}
