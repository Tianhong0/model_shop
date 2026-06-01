package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.Result;
import org.majun.backend.dto.OrderBatchPayCreateRequest;
import org.majun.backend.dto.OrderPayCreateRequest;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.OrderPaymentService;
import org.majun.backend.vo.OrderBatchPayCreateResponse;
import org.majun.backend.vo.OrderBatchPayStatusVO;
import org.majun.backend.vo.OrderPayCreateResponse;
import org.majun.backend.vo.OrderPayStatusVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Order Payment", description = "Order payment APIs")
@RestController
@RequestMapping("/api/orders/pay")
@RequiredArgsConstructor
@Slf4j
/**
 * 订单支付控制器
 */
public class OrderPaymentController {

    private final OrderPaymentService orderPaymentService;

    /** 创建支付宝APP支付 */
    @Operation(summary = "Create app payment", description = "Create Alipay app payment order string")
    @PostMapping("/app/create")
    public Result<OrderPayCreateResponse> createAppPayment(@AuthenticationPrincipal LoginUser loginUser,
                                                            @Valid @RequestBody OrderPayCreateRequest request) {
        return Result.success(orderPaymentService.createAppPayOrder(request, loginUser.getId()));
    }

    /** 创建支付宝APP批量支付 */
    @Operation(summary = "Create batch app payment", description = "Create Alipay app payment order string for multiple orders")
    @PostMapping("/app/create-batch")
    public Result<OrderBatchPayCreateResponse> createBatchAppPayment(@AuthenticationPrincipal LoginUser loginUser,
                                                                     @Valid @RequestBody OrderBatchPayCreateRequest request) {
        return Result.success(orderPaymentService.createBatchAppPayOrder(request, loginUser.getId()));
    }

    // ==================== 钱包支付 ====================
    /** 钱包支付 */
    @PostMapping("/wallet/pay")
    public Result<OrderPayStatusVO> payOrderByWallet(@AuthenticationPrincipal LoginUser loginUser,
                                                     @Valid @RequestBody OrderPayCreateRequest request) {
        return Result.success(orderPaymentService.payOrderByWallet(request, loginUser.getId()));
    }

    /** 钱包批量支付 */
    @Operation(summary = "Wallet pay batch orders", description = "Use wallet balance to pay multiple orders")
    @PostMapping("/wallet/pay-batch")
    public Result<OrderBatchPayStatusVO> payBatchByWallet(@AuthenticationPrincipal LoginUser loginUser,
                                                           @Valid @RequestBody OrderBatchPayCreateRequest request) {
        return Result.success(orderPaymentService.payBatchByWallet(request, loginUser.getId()));
    }

    /** 查询支付状态 */
    @Operation(summary = "Query payment status", description = "Query payment status by order ID")
    @GetMapping("/status/{orderId}")
    public Result<OrderPayStatusVO> queryPayStatus(@AuthenticationPrincipal LoginUser loginUser,
                                                    @PathVariable Long orderId) {
        return Result.success(orderPaymentService.queryPayStatus(orderId, loginUser.getId()));
    }

    /** 查询批量支付状态 */
    @Operation(summary = "Query batch payment status", description = "Query payment status by batch ID")
    @GetMapping("/status/batch/{batchId}")
    public Result<OrderBatchPayStatusVO> queryBatchPayStatus(@AuthenticationPrincipal LoginUser loginUser,
                                                              @PathVariable Long batchId) {
        return Result.success(orderPaymentService.queryBatchPayStatus(batchId, loginUser.getId()));
    }

    /** 同步支付状态 */
    @Operation(summary = "Sync payment status", description = "Active query Alipay and sync local status")
    @PostMapping("/sync/{orderId}")
    public Result<OrderPayStatusVO> syncPayStatus(@AuthenticationPrincipal LoginUser loginUser,
                                                  @PathVariable Long orderId) {
        return Result.success(orderPaymentService.syncPayStatus(orderId, loginUser.getId()));
    }

    /** 同步批量支付状态 */
    @Operation(summary = "Sync batch payment status", description = "Active query Alipay and sync local batch status")
    @PostMapping("/sync/batch/{batchId}")
    public Result<OrderBatchPayStatusVO> syncBatchPayStatus(@AuthenticationPrincipal LoginUser loginUser,
                                                             @PathVariable Long batchId) {
        return Result.success(orderPaymentService.syncBatchPayStatus(batchId, loginUser.getId()));
    }

    /** 支付宝异步通知回调 */
    @Operation(summary = "Alipay notify", description = "Alipay async notify callback")
    @PostMapping("/alipay/notify")
    public String alipayNotify(HttpServletRequest request) {
        System.out.println(request.getContentType());
        try {
            Map<String, String> params = new HashMap<>();
            request.getParameterMap().forEach((key, values) -> {
                if (values != null && values.length > 0) {
                    params.put(key, values[0]);
                }
            });
            boolean success = orderPaymentService.handleAlipayNotify(params);
            return success ? "success" : "failure";
        } catch (Exception ex) {
            log.error("支付宝回调处理失败", ex);
            return "failure";
        }
    }
}
