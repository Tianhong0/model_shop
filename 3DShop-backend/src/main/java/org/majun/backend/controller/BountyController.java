package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.Result;
import org.majun.backend.dto.BountyAcceptRequest;
import org.majun.backend.dto.BountyBidCreateRequest;
import org.majun.backend.dto.BountyBidUpdateRequest;
import org.majun.backend.dto.BountyDeliverySubmitRequest;
import org.majun.backend.dto.BountyMessageSendRequest;
import org.majun.backend.dto.BountyPickBidRequest;
import org.majun.backend.dto.BountyPriceChangeConfirmRequest;
import org.majun.backend.dto.BountyPriceChangeRequest;
import org.majun.backend.dto.BountyTaskCreateRequest;
import org.majun.backend.dto.BountyTaskQueryRequest;
import org.majun.backend.dto.BountyTaskReviewRequest;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.BountyPaymentService;
import org.majun.backend.service.BountyService;
import org.majun.backend.vo.BountyMessageVO;
import org.majun.backend.vo.BountyPayCreateResponse;
import org.majun.backend.vo.BountyPayStatusVO;
import org.majun.backend.vo.BountyTaskDetailVO;
import org.majun.backend.vo.BountyTaskListVO;
import org.majun.backend.vo.PageResult;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Bounty", description = "悬赏任务接口")
@RestController
@RequestMapping("/api/bounty")
@RequiredArgsConstructor
@Slf4j
public class BountyController {

    private final BountyService bountyService;
    private final BountyPaymentService bountyPaymentService;

    @Operation(summary = "发布悬赏任务")
    @PostMapping("/task/create")
    public Result<Long> createTask(@AuthenticationPrincipal LoginUser loginUser,
                                   @Valid @RequestBody BountyTaskCreateRequest request) {
        return Result.success(bountyService.createTask(request, loginUser.getId()));
    }

    @Operation(summary = "悬赏任务分页")
    @PostMapping("/task/page")
    public Result<PageResult<BountyTaskListVO>> pageTasks(@AuthenticationPrincipal LoginUser loginUser,
                                                           @RequestBody(required = false) BountyTaskQueryRequest request) {
        if (request == null) {
            request = new BountyTaskQueryRequest();
        }
        return Result.success(bountyService.pageTasks(request, loginUser.getId()));
    }

    @Operation(summary = "悬赏任务详情")
    @GetMapping("/task/detail/{taskId}")
    public Result<BountyTaskDetailVO> taskDetail(@AuthenticationPrincipal LoginUser loginUser,
                                                  @PathVariable Long taskId) {
        return Result.success(bountyService.getTaskDetail(taskId, loginUser.getId()));
    }

    @Operation(summary = "管理员查看悬赏任务详情")
    @GetMapping("/admin/task/detail/{taskId}")
    public Result<BountyTaskDetailVO> adminTaskDetail(@AuthenticationPrincipal LoginUser loginUser,
                                                       @PathVariable Long taskId) {
        if (!isAdmin(loginUser)) {
            return Result.fail(403, "无管理员权限");
        }
        return Result.success(bountyService.getTaskDetailForAdmin(taskId));
    }

    @Operation(summary = "管理员审核悬赏任务")
    @PostMapping("/admin/task/review")
    public Result<Void> reviewTask(@AuthenticationPrincipal LoginUser loginUser,
                                   @Valid @RequestBody BountyTaskReviewRequest request) {
        if (!isAdmin(loginUser)) {
            return Result.fail(403, "无管理员权限");
        }
        bountyService.reviewTask(request, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "提交竞标")
    @PostMapping("/bid/create")
    public Result<Long> createBid(@AuthenticationPrincipal LoginUser loginUser,
                                  @Valid @RequestBody BountyBidCreateRequest request) {
        return Result.success(bountyService.createBid(request, loginUser.getId()));
    }

    @Operation(summary = "修改竞标")
    @PostMapping("/bid/update")
    public Result<Void> updateBid(@AuthenticationPrincipal LoginUser loginUser,
                                  @Valid @RequestBody BountyBidUpdateRequest request) {
        bountyService.updateBid(request, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "撤回竞标")
    @PostMapping("/bid/withdraw/{bidId}")
    public Result<Void> withdrawBid(@AuthenticationPrincipal LoginUser loginUser,
                                    @PathVariable Long bidId) {
        bountyService.withdrawBid(bidId, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "选中标")
    @PostMapping("/bid/pick")
    public Result<Void> pickBid(@AuthenticationPrincipal LoginUser loginUser,
                                @Valid @RequestBody BountyPickBidRequest request) {
        bountyService.pickBid(request, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "提交交付")
    @PostMapping("/delivery/submit")
    public Result<Long> submitDelivery(@AuthenticationPrincipal LoginUser loginUser,
                                       @Valid @RequestBody BountyDeliverySubmitRequest request) {
        return Result.success(bountyService.submitDelivery(request, loginUser.getId()));
    }

    @Operation(summary = "验收交付")
    @PostMapping("/delivery/accept")
    public Result<Void> acceptDelivery(@AuthenticationPrincipal LoginUser loginUser,
                                       @Valid @RequestBody BountyAcceptRequest request) {
        bountyService.acceptDelivery(request, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "发起改价")
    @PostMapping("/price/change/apply")
    public Result<Long> applyPriceChange(@AuthenticationPrincipal LoginUser loginUser,
                                         @Valid @RequestBody BountyPriceChangeRequest request) {
        return Result.success(bountyService.applyPriceChange(request, loginUser.getId()));
    }

    @Operation(summary = "确认改价")
    @PostMapping("/price/change/confirm")
    public Result<Void> confirmPriceChange(@AuthenticationPrincipal LoginUser loginUser,
                                           @Valid @RequestBody BountyPriceChangeConfirmRequest request) {
        bountyService.confirmPriceChange(request, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "创建改价补差支付")
    @PostMapping("/price/change/pay/create/{priceChangeId}")
    public Result<BountyPayCreateResponse> createPriceIncreasePay(@AuthenticationPrincipal LoginUser loginUser,
                                                                   @PathVariable Long priceChangeId) {
        return Result.success(bountyPaymentService.createPriceIncreasePayOrder(priceChangeId, loginUser.getId()));
    }

    @Operation(summary = "查询改价补差支付状态")
    @GetMapping("/price/change/pay/status/{priceChangeId}")
    public Result<BountyPayStatusVO> queryPriceIncreasePay(@AuthenticationPrincipal LoginUser loginUser,
                                                            @PathVariable Long priceChangeId) {
        return Result.success(bountyPaymentService.queryPriceIncreasePayStatus(priceChangeId, loginUser.getId()));
    }

    @Operation(summary = "按任务查询改价补差支付状态")
    @GetMapping("/price/change/pay/status/by-task/{taskId}")
    public Result<BountyPayStatusVO> queryPriceIncreasePayByTask(@AuthenticationPrincipal LoginUser loginUser,
                                                                  @PathVariable Long taskId) {
        return Result.success(bountyPaymentService.queryPriceIncreasePayStatusByTask(taskId, loginUser.getId(), isAdmin(loginUser)));
    }

    @Operation(summary = "同步改价补差支付状态")
    @PostMapping("/price/change/pay/sync/{priceChangeId}")
    public Result<BountyPayStatusVO> syncPriceIncreasePay(@AuthenticationPrincipal LoginUser loginUser,
                                                           @PathVariable Long priceChangeId) {
        return Result.success(bountyPaymentService.syncPriceIncreasePayStatus(priceChangeId, loginUser.getId()));
    }

    @Operation(summary = "按任务同步改价补差支付状态")
    @PostMapping("/price/change/pay/sync/by-task/{taskId}")
    public Result<BountyPayStatusVO> syncPriceIncreasePayByTask(@AuthenticationPrincipal LoginUser loginUser,
                                                                 @PathVariable Long taskId) {
        return Result.success(bountyPaymentService.syncPriceIncreasePayStatusByTask(taskId, loginUser.getId(), isAdmin(loginUser)));
    }

    @Operation(summary = "悬赏补差支付支付宝回调")
    @PostMapping("/pay/alipay/notify")
    public String alipayNotify(HttpServletRequest request) {
        try {
            Map<String, String> params = new HashMap<>();
            request.getParameterMap().forEach((key, values) -> {
                if (values != null && values.length > 0) {
                    params.put(key, values[0]);
                }
            });
            boolean success = bountyPaymentService.handleAlipayNotify(params);
            return success ? "success" : "failure";
        } catch (Exception ex) {
            log.error("悬赏补差支付宝回调处理失败", ex);
            return "failure";
        }
    }

    private boolean isAdmin(LoginUser loginUser) {
        if (loginUser == null || loginUser.getAuthorities() == null) {
            return false;
        }
        return loginUser.getAuthorities().stream()
                .anyMatch(item -> "ROLE_ADMIN".equals(item.getAuthority()));
    }

    @Operation(summary = "发送消息")
    @PostMapping("/message/send")
    public Result<Long> sendMessage(@AuthenticationPrincipal LoginUser loginUser,
                                    @Valid @RequestBody BountyMessageSendRequest request) {
        return Result.success(bountyService.sendMessage(request, loginUser.getId(), "USER"));
    }

    @Operation(summary = "消息分页")
    @GetMapping("/message/page")
    public Result<PageResult<BountyMessageVO>> pageMessages(@AuthenticationPrincipal LoginUser loginUser,
                                                            @RequestParam Long taskId,
                                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                                            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(bountyService.pageMessages(taskId, pageNum, pageSize, loginUser.getId()));
    }
}
