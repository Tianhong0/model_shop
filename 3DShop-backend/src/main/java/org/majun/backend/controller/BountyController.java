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
import org.majun.backend.dto.BountyCancelRequest;
import org.majun.backend.dto.BountyCancelReviewRequest;
import org.majun.backend.dto.BountyDeliverySubmitRequest;
import org.majun.backend.dto.BountyMessageSendRequest;
import org.majun.backend.dto.BountyPickBidRequest;
import org.majun.backend.dto.BountyPriceChangeConfirmRequest;
import org.majun.backend.dto.BountyPriceChangeRequest;
import org.majun.backend.dto.BountyRatingAppealCreateRequest;
import org.majun.backend.dto.BountyRatingAppealReviewRequest;
import org.majun.backend.dto.BountyRatingCreateRequest;
import org.majun.backend.dto.BountyTaskCreateRequest;
import org.majun.backend.dto.BountyTaskQueryRequest;
import org.majun.backend.dto.BountyTaskResubmitRequest;
import org.majun.backend.dto.BountyTaskReviewRequest;
import org.majun.backend.annotation.OperationLog;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.BountyPaymentService;
import org.majun.backend.service.BountyService;
import org.majun.backend.vo.BountyMessageVO;
import org.majun.backend.vo.BountyPayCreateResponse;
import org.majun.backend.vo.BountyPayStatusVO;
import org.majun.backend.vo.BountyRatingAppealVO;
import org.majun.backend.vo.BountyRatingVO;
import org.majun.backend.vo.BountyTaskDetailVO;
import org.majun.backend.vo.BountyTaskListVO;
import org.majun.backend.vo.DesignerReputationVO;
import org.majun.backend.vo.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
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
/**
 * 悬赏任务控制器
 */
public class BountyController {

    private final BountyService bountyService;
    private final BountyPaymentService bountyPaymentService;

    /** 发布悬赏任务 */
    @Operation(summary = "发布悬赏任务")
    @PostMapping("/task/create")
    public Result<Long> createTask(@AuthenticationPrincipal LoginUser loginUser,
                                   @Valid @RequestBody BountyTaskCreateRequest request) {
        return Result.success(bountyService.createTask(request, loginUser.getId()));
    }

    /** 驳回后重新提交审核 */
    @Operation(summary = "驳回后重新提交审核")
    @PostMapping("/task/resubmit")
    public Result<Void> resubmitTask(@AuthenticationPrincipal LoginUser loginUser,
                                     @Valid @RequestBody BountyTaskResubmitRequest request) {
        bountyService.resubmitTask(request, loginUser.getId());
        return Result.success();
    }

    /** 悬赏任务分页 */
    @Operation(summary = "悬赏任务分页")
    @PostMapping("/task/page")
    public Result<PageResult<BountyTaskListVO>> pageTasks(@AuthenticationPrincipal LoginUser loginUser,
                                                           @RequestBody(required = false) BountyTaskQueryRequest request) {
        if (request == null) {
            request = new BountyTaskQueryRequest();
        }
        return Result.success(bountyService.pageTasks(request, loginUser.getId()));
    }

    /** 悬赏任务详情 */
    @Operation(summary = "悬赏任务详情")
    @GetMapping("/task/detail/{taskId}")
    public Result<BountyTaskDetailVO> taskDetail(@AuthenticationPrincipal LoginUser loginUser,
                                                  @PathVariable Long taskId) {
        return Result.success(bountyService.getTaskDetail(taskId, loginUser.getId()));
    }

    /** 管理员分页查询悬赏任务 */
    @Operation(summary = "管理员分页查询悬赏任务")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/task/page")
    public Result<PageResult<BountyTaskListVO>> adminPageTasks(@RequestBody(required = false) BountyTaskQueryRequest request) {
        if (request == null) {
            request = new BountyTaskQueryRequest();
        }
        return Result.success(bountyService.pageTasksForAdmin(request));
    }

    /** 管理员查看悬赏任务详情 */
    @Operation(summary = "管理员查看悬赏任务详情")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/admin/task/detail/{taskId}")
    public Result<BountyTaskDetailVO> adminTaskDetail(@PathVariable Long taskId) {
        return Result.success(bountyService.getTaskDetailForAdmin(taskId));
    }

    /** 管理员审核悬赏任务 */
    @Operation(summary = "管理员审核悬赏任务")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @OperationLog(type = "REVIEW", module = "悬赏管理", description = "审核悬赏任务", targetType = "BOUNTY_TASK")
    @PostMapping("/admin/task/review")
    public Result<Void> reviewTask(@AuthenticationPrincipal LoginUser loginUser,
                                   @Valid @RequestBody BountyTaskReviewRequest request) {
        bountyService.reviewTask(request, loginUser.getId());
        return Result.success();
    }

    /** 管理员审核取消悬赏 */
    @Operation(summary = "管理员审核取消悬赏")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @OperationLog(type = "REVIEW", module = "悬赏管理", description = "审核取消悬赏", targetType = "BOUNTY_TASK")
    @PostMapping("/admin/task/cancel-review")
    public Result<Void> reviewCancelTask(@AuthenticationPrincipal LoginUser loginUser,
                                         @Valid @RequestBody BountyCancelReviewRequest request) {
        bountyService.reviewCancelTask(request, loginUser.getId());
        return Result.success();
    }

    /** 提交竞标 */
    @Operation(summary = "提交竞标")
    @PostMapping("/bid/create")
    public Result<Long> createBid(@AuthenticationPrincipal LoginUser loginUser,
                                  @Valid @RequestBody BountyBidCreateRequest request) {
        return Result.success(bountyService.createBid(request, loginUser.getId()));
    }

    /** 修改竞标 */
    @Operation(summary = "修改竞标")
    @PostMapping("/bid/update")
    public Result<Void> updateBid(@AuthenticationPrincipal LoginUser loginUser,
                                  @Valid @RequestBody BountyBidUpdateRequest request) {
        bountyService.updateBid(request, loginUser.getId());
        return Result.success();
    }

    /** 撤回竞标 */
    @Operation(summary = "撤回竞标")
    @PostMapping("/bid/withdraw/{bidId}")
    public Result<Void> withdrawBid(@AuthenticationPrincipal LoginUser loginUser,
                                    @PathVariable Long bidId) {
        bountyService.withdrawBid(bidId, loginUser.getId());
        return Result.success();
    }

    /** 选中标 */
    @Operation(summary = "选中标")
    @PostMapping("/bid/pick")
    public Result<Void> pickBid(@AuthenticationPrincipal LoginUser loginUser,
                                @Valid @RequestBody BountyPickBidRequest request) {
        bountyService.pickBid(request, loginUser.getId());
        return Result.success();
    }

    /** 提交交付 */
    @Operation(summary = "提交交付")
    @PostMapping("/delivery/submit")
    public Result<Long> submitDelivery(@AuthenticationPrincipal LoginUser loginUser,
                                       @Valid @RequestBody BountyDeliverySubmitRequest request) {
        return Result.success(bountyService.submitDelivery(request, loginUser.getId()));
    }

    /** 验收交付 */
    @Operation(summary = "验收交付")
    @PostMapping("/delivery/accept")
    public Result<Void> acceptDelivery(@AuthenticationPrincipal LoginUser loginUser,
                                       @Valid @RequestBody BountyAcceptRequest request) {
        bountyService.acceptDelivery(request, loginUser.getId());
        return Result.success();
    }

    /** 申请取消悬赏 */
    @Operation(summary = "申请取消悬赏")
    @PostMapping("/task/cancel-request")
    public Result<Void> requestCancelTask(@AuthenticationPrincipal LoginUser loginUser,
                                          @Valid @RequestBody BountyCancelRequest request) {
        bountyService.requestCancelTask(request, loginUser.getId());
        return Result.success();
    }

    /** 发起改价 */
    @Operation(summary = "发起改价")
    @PostMapping("/price/change/apply")
    public Result<Long> applyPriceChange(@AuthenticationPrincipal LoginUser loginUser,
                                         @Valid @RequestBody BountyPriceChangeRequest request) {
        return Result.success(bountyService.applyPriceChange(request, loginUser.getId()));
    }

    /** 确认改价 */
    @Operation(summary = "确认改价")
    @PostMapping("/price/change/confirm")
    public Result<Void> confirmPriceChange(@AuthenticationPrincipal LoginUser loginUser,
                                           @Valid @RequestBody BountyPriceChangeConfirmRequest request) {
        bountyService.confirmPriceChange(request, loginUser.getId());
        return Result.success();
    }

    // ==================== 托管金支付接口 ====================

    /** 创建托管金支付 */
    @Operation(summary = "创建托管金支付")
    @PostMapping("/escrow/pay/create/{taskId}")
    public Result<BountyPayCreateResponse> createEscrowPay(@AuthenticationPrincipal LoginUser loginUser,
                                                           @PathVariable Long taskId) {
        return Result.success(bountyPaymentService.createEscrowPayOrder(taskId, loginUser.getId()));
    }

    /** 查询托管金支付状态 */
    @Operation(summary = "查询托管金支付状态")
    @GetMapping("/escrow/pay/status/{taskId}")
    public Result<BountyPayStatusVO> queryEscrowPay(@AuthenticationPrincipal LoginUser loginUser,
                                                     @PathVariable Long taskId) {
        return Result.success(bountyPaymentService.queryEscrowPayStatus(taskId, loginUser.getId()));
    }

    /** 同步托管金支付状态 */
    @Operation(summary = "同步托管金支付状态")
    @PostMapping("/escrow/pay/sync/{taskId}")
    public Result<BountyPayStatusVO> syncEscrowPay(@AuthenticationPrincipal LoginUser loginUser,
                                                    @PathVariable Long taskId) {
        return Result.success(bountyPaymentService.syncEscrowPayStatus(taskId, loginUser.getId()));
    }

    // ==================== 改价补差支付接口 ====================

    /** 创建改价补差支付 */
    @Operation(summary = "创建改价补差支付")
    @PostMapping("/price/change/pay/create/{priceChangeId}")
    public Result<BountyPayCreateResponse> createPriceIncreasePay(@AuthenticationPrincipal LoginUser loginUser,
                                                                   @PathVariable Long priceChangeId) {
        return Result.success(bountyPaymentService.createPriceIncreasePayOrder(priceChangeId, loginUser.getId()));
    }

    /** 查询改价补差支付状态 */
    @Operation(summary = "查询改价补差支付状态")
    @GetMapping("/price/change/pay/status/{priceChangeId}")
    public Result<BountyPayStatusVO> queryPriceIncreasePay(@AuthenticationPrincipal LoginUser loginUser,
                                                            @PathVariable Long priceChangeId) {
        return Result.success(bountyPaymentService.queryPriceIncreasePayStatus(priceChangeId, loginUser.getId()));
    }

    /** 按任务查询改价补差支付状态 */
    @Operation(summary = "按任务查询改价补差支付状态")
    @GetMapping("/price/change/pay/status/by-task/{taskId}")
    public Result<BountyPayStatusVO> queryPriceIncreasePayByTask(@AuthenticationPrincipal LoginUser loginUser,
                                                                  @PathVariable Long taskId) {
        return Result.success(bountyPaymentService.queryPriceIncreasePayStatusByTask(taskId, loginUser.getId(), isAdmin(loginUser)));
    }

    /** 同步改价补差支付状态 */
    @Operation(summary = "同步改价补差支付状态")
    @PostMapping("/price/change/pay/sync/{priceChangeId}")
    public Result<BountyPayStatusVO> syncPriceIncreasePay(@AuthenticationPrincipal LoginUser loginUser,
                                                           @PathVariable Long priceChangeId) {
        return Result.success(bountyPaymentService.syncPriceIncreasePayStatus(priceChangeId, loginUser.getId()));
    }

    /** 按任务同步改价补差支付状态 */
    @Operation(summary = "按任务同步改价补差支付状态")
    @PostMapping("/price/change/pay/sync/by-task/{taskId}")
    public Result<BountyPayStatusVO> syncPriceIncreasePayByTask(@AuthenticationPrincipal LoginUser loginUser,
                                                                 @PathVariable Long taskId) {
        return Result.success(bountyPaymentService.syncPriceIncreasePayStatusByTask(taskId, loginUser.getId(), isAdmin(loginUser)));
    }

    /** 悬赏补差支付支付宝回调 */
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

    /** 发送消息 */
    @Operation(summary = "发送消息")
    @PostMapping("/message/send")
    public Result<Long> sendMessage(@AuthenticationPrincipal LoginUser loginUser,
                                    @Valid @RequestBody BountyMessageSendRequest request) {
        return Result.success(bountyService.sendMessage(request, loginUser.getId(), "USER"));
    }

    /** 消息分页 */
    @Operation(summary = "消息分页")
    @GetMapping("/message/page")
    public Result<PageResult<BountyMessageVO>> pageMessages(@AuthenticationPrincipal LoginUser loginUser,
                                                            @RequestParam Long taskId,
                                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                                            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(bountyService.pageMessages(taskId, pageNum, pageSize, loginUser.getId()));
    }

    // ==================== 评价相关接口 ====================

    /** 创建悬赏评价 */
    @Operation(summary = "创建悬赏评价")
    @PostMapping("/rating/create")
    public Result<Long> createRating(@AuthenticationPrincipal LoginUser loginUser,
                                     @Valid @RequestBody BountyRatingCreateRequest request) {
        return Result.success(bountyService.createRating(request, loginUser.getId()));
    }

    /** 获取任务评价 */
    @Operation(summary = "获取任务评价")
    @GetMapping("/rating/task/{taskId}")
    public Result<BountyRatingVO> getRatingByTask(@AuthenticationPrincipal LoginUser loginUser,
                                                   @PathVariable Long taskId) {
        return Result.success(bountyService.getRatingByTask(taskId, loginUser.getId()));
    }

    /** 获取设计者评价列表 */
    @Operation(summary = "获取设计者评价列表")
    @GetMapping("/rating/designer/{designerId}")
    public Result<PageResult<BountyRatingVO>> getDesignerRatings(@PathVariable Long designerId,
                                                                  @RequestParam(defaultValue = "1") Integer pageNum,
                                                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(bountyService.getDesignerRatings(designerId, pageNum, pageSize));
    }

    // ==================== 申诉相关接口 ====================

    /** 创建评价申诉 */
    @Operation(summary = "创建评价申诉")
    @PostMapping("/rating/appeal/create")
    public Result<Long> createRatingAppeal(@AuthenticationPrincipal LoginUser loginUser,
                                           @Valid @RequestBody BountyRatingAppealCreateRequest request) {
        return Result.success(bountyService.createRatingAppeal(request, loginUser.getId()));
    }

    /** 获取我的申诉列表 */
    @Operation(summary = "获取我的申诉列表")
    @GetMapping("/rating/appeal/my")
    public Result<PageResult<BountyRatingAppealVO>> getMyAppeals(@AuthenticationPrincipal LoginUser loginUser,
                                                                  @RequestParam(defaultValue = "1") Integer pageNum,
                                                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(bountyService.getMyAppeals(loginUser.getId(), pageNum, pageSize));
    }

    /** 管理员获取申诉列表 */
    @Operation(summary = "管理员获取申诉列表")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/admin/rating/appeal/list")
    public Result<PageResult<BountyRatingAppealVO>> getAllAppeals(@RequestParam(defaultValue = "1") Integer pageNum,
                                                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                                                   @RequestParam(required = false) Integer status) {
        return Result.success(bountyService.getAllAppeals(pageNum, pageSize, status));
    }

    /** 管理员审核申诉 */
    @Operation(summary = "管理员审核申诉")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @OperationLog(type = "REVIEW", module = "悬赏管理", description = "审核评价申诉", targetType = "RATING_APPEAL")
    @PostMapping("/admin/rating/appeal/review")
    public Result<Void> reviewRatingAppeal(@AuthenticationPrincipal LoginUser loginUser,
                                           @Valid @RequestBody BountyRatingAppealReviewRequest request) {
        bountyService.reviewRatingAppeal(request, loginUser.getId());
        return Result.success();
    }

    // ==================== 信誉相关接口 ====================

    /** 获取设计者信誉信息 */
    @Operation(summary = "获取设计者信誉信息")
    @GetMapping("/reputation/{designerId}")
    public Result<DesignerReputationVO> getDesignerReputation(@PathVariable Long designerId) {
        return Result.success(bountyService.getDesignerReputation(designerId));
    }
}
