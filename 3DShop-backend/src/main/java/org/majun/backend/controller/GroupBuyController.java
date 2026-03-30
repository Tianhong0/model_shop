package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.Result;
import org.majun.backend.dto.*;
import org.majun.backend.entity.SysBatchPrintDiscount;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.GroupBuyService;
import org.majun.backend.vo.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端拼团接口
 */
@Tag(name = "GroupBuy", description = "拼团API")
@RestController
@RequestMapping("/api/group-buy")
@RequiredArgsConstructor
public class GroupBuyController {

    private final GroupBuyService groupBuyService;

    @Operation(summary = "获取拼团活动列表")
    @PostMapping("/activities")
    public Result<PageResult<GroupBuyActivityVO>> listActivities(@RequestBody(required = false) GroupBuyActivityQueryRequest request) {
        if (request == null) {
            request = new GroupBuyActivityQueryRequest();
        }
        return Result.success(groupBuyService.listUserActivities(request));
    }

    @Operation(summary = "获取活动详情")
    @GetMapping("/activity/{activityId}")
    public Result<GroupBuyActivityDetailVO> getActivityDetail(@PathVariable Long activityId) {
        return Result.success(groupBuyService.getUserActivityDetail(activityId));
    }

    @Operation(summary = "获取活动下进行中的拼团列表")
    @GetMapping("/activity/{activityId}/ongoing-groups")
    public Result<List<GroupBuyGroupVO>> getOngoingGroupsByActivity(
            @PathVariable Long activityId,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return Result.success(groupBuyService.getOngoingGroupsByActivity(activityId, limit));
    }

    @Operation(summary = "发起拼团")
    @PostMapping("/create")
    public Result<GroupBuyCreateResponse> createGroupBuy(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody GroupBuyCreateRequest request) {
        return Result.success(groupBuyService.createGroupBuy(loginUser.getId(), request));
    }

    @Operation(summary = "参与拼团")
    @PostMapping("/join")
    public Result<GroupBuyJoinResponse> joinGroupBuy(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody GroupBuyJoinRequest request) {
        return Result.success(groupBuyService.joinGroupBuy(loginUser.getId(), request));
    }

    @Operation(summary = "获取拼团详情")
    @GetMapping("/group/{groupId}")
    public Result<GroupBuyGroupDetailVO> getGroupDetail(
            @PathVariable Long groupId,
            @AuthenticationPrincipal LoginUser loginUser) {
        Long userId = loginUser != null ? loginUser.getId() : null;
        return Result.success(groupBuyService.getGroupDetail(groupId, userId));
    }

    @Operation(summary = "通过分享码获取拼团详情")
    @GetMapping("/group/by-code/{shareCode}")
    public Result<GroupBuyGroupDetailVO> getGroupByShareCode(
            @PathVariable String shareCode,
            @AuthenticationPrincipal LoginUser loginUser) {
        Long userId = loginUser != null ? loginUser.getId() : null;
        return Result.success(groupBuyService.getGroupByShareCode(shareCode, userId));
    }

    @Operation(summary = "获取我参与的拼团列表")
    @PostMapping("/my-groups")
    public Result<PageResult<GroupBuyGroupVO>> getMyGroups(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestBody(required = false) GroupBuyActivityQueryRequest request) {
        if (request == null) {
            request = new GroupBuyActivityQueryRequest();
        }
        return Result.success(groupBuyService.getMyGroups(loginUser.getId(), request));
    }

    @Operation(summary = "取消拼团")
    @DeleteMapping("/cancel/{groupId}")
    public Result<Void> cancelGroupBuy(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long groupId) {
        groupBuyService.cancelGroupBuy(loginUser.getId(), groupId);
        return Result.success();
    }

    @Operation(summary = "计算批量打印价格")
    @PostMapping("/calculate-batch-price")
    public Result<BatchPriceResultVO> calculateBatchPrice(@Valid @RequestBody BatchPriceCalculateRequest request) {
        return Result.success(groupBuyService.calculateBatchPrice(request));
    }

    @Operation(summary = "为参与者创建订单")
    @PostMapping("/participant/{participantId}/create-order")
    public Result<Long> createOrderForParticipant(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long participantId) {
        return Result.success(groupBuyService.createOrderForParticipant(participantId, loginUser.getId()));
    }

    @Operation(summary = "获取批量打印折扣配置")
    @GetMapping("/batch-discount/list")
    public Result<List<SysBatchPrintDiscount>> getBatchDiscountList() {
        return Result.success(groupBuyService.getBatchDiscountList());
    }
}
