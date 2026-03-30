package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.Result;
import org.majun.backend.dto.WalletLedgerQueryRequest;
import org.majun.backend.dto.WalletRechargeRequest;
import org.majun.backend.dto.WalletWithdrawApplyRequest;
import org.majun.backend.dto.WalletWithdrawAuditRequest;
import org.majun.backend.dto.WalletWithdrawPayRequest;
import org.majun.backend.dto.WalletWithdrawQueryRequest;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.impl.WalletService;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.WalletAccountVO;
import org.majun.backend.vo.WalletFrozenDetailVO;
import org.majun.backend.vo.WalletLedgerVO;
import org.majun.backend.vo.WalletRechargePayCreateResponse;
import org.majun.backend.vo.WalletRechargeStatusVO;
import org.majun.backend.vo.WalletWithdrawVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Wallet", description = "钱包与提现接口")
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Slf4j
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/account")
    @Operation(summary = "钱包账户概览")
    public Result<WalletAccountVO> getAccount(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(walletService.getAccount(loginUser.getId()));
    }

    @GetMapping("/frozen/list")
    @Operation(summary = "冻结资金记录列表")
    public Result<java.util.List<WalletFrozenDetailVO>> listFrozenRecords(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(walletService.listFrozenRecords(loginUser.getId()));
    }

    @PostMapping("/ledger/page")
    @Operation(summary = "钱包流水分页")
    public Result<PageResult<WalletLedgerVO>> pageLedger(@AuthenticationPrincipal LoginUser loginUser,
                                                         @RequestBody(required = false) WalletLedgerQueryRequest request) {
        WalletLedgerQueryRequest req = request == null ? new WalletLedgerQueryRequest() : request;
        return Result.success(walletService.pageLedger(req, loginUser.getId()));
    }

    @PostMapping("/recharge/app/create")
    @Operation(summary = "钱包充值下单")
    public Result<WalletRechargePayCreateResponse> createRechargeOrder(@AuthenticationPrincipal LoginUser loginUser,
                                                                       @Valid @RequestBody WalletRechargeRequest request) {
        return Result.success(walletService.createRechargePayOrder(request, loginUser.getId()));
    }

    @PostMapping("/recharge/sync/{outTradeNo}")
    @Operation(summary = "同步充值状态")
    public Result<WalletRechargeStatusVO> syncRechargeStatus(@AuthenticationPrincipal LoginUser loginUser,
                                                             @PathVariable String outTradeNo) {
        return Result.success(walletService.syncRechargeStatus(outTradeNo, loginUser.getId()));
    }

    @PostMapping("/recharge/alipay/notify")
    @Operation(summary = "支付宝充值回调")
    public String rechargeAlipayNotify(HttpServletRequest request) {
        try {
            return walletService.handleRechargeAlipayNotify(request) ? "success" : "failure";
        } catch (Exception ex) {
            log.error("钱包充值支付宝回调处理失败", ex);
            return "failure";
        }
    }

    @PostMapping("/withdraw/apply")
    @Operation(summary = "申请提现")
    public Result<String> applyWithdraw(@AuthenticationPrincipal LoginUser loginUser,
                                        @Valid @RequestBody WalletWithdrawApplyRequest request) {
        return Result.success(walletService.applyWithdraw(request, loginUser.getId()));
    }

    @PostMapping("/withdraw/my/page")
    @Operation(summary = "我的提现记录")
    public Result<PageResult<WalletWithdrawVO>> pageMyWithdraw(@AuthenticationPrincipal LoginUser loginUser,
                                                               @RequestBody(required = false) WalletWithdrawQueryRequest request) {
        WalletWithdrawQueryRequest req = request == null ? new WalletWithdrawQueryRequest() : request;
        return Result.success(walletService.pageMyWithdraw(req, loginUser.getId()));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/withdraw/page")
    @Operation(summary = "管理员-提现记录分页")
    public Result<PageResult<WalletWithdrawVO>> pageAdminWithdraw(@RequestBody(required = false) WalletWithdrawQueryRequest request) {
        WalletWithdrawQueryRequest req = request == null ? new WalletWithdrawQueryRequest() : request;
        return Result.success(walletService.pageAdminWithdraw(req));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/withdraw/audit")
    @Operation(summary = "管理员-提现审核")
    public Result<Void> auditWithdraw(@AuthenticationPrincipal LoginUser loginUser,
                                      @Valid @RequestBody WalletWithdrawAuditRequest request) {
        walletService.auditWithdraw(request, loginUser.getId());
        return Result.success();
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/withdraw/pay")
    @Operation(summary = "管理员-提现打款")
    public Result<Void> payWithdraw(@AuthenticationPrincipal LoginUser loginUser,
                                    @Valid @RequestBody WalletWithdrawPayRequest request) {
        walletService.payWithdraw(request, loginUser.getId());
        return Result.success();
    }
}
