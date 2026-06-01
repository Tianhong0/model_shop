package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.Result;
import org.majun.backend.dto.DesignerSettlementQueryRequest;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.DesignerSettlementService;
import org.majun.backend.vo.DesignerSettlementVO;
import org.majun.backend.vo.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 设计师分润控制器 — 管理设计师收益结算、分润记录和提现流水查询
 */
@Tag(name = "设计师分润", description = "设计师分润结算相关接口")
@RestController
@RequestMapping("/api/designer")
@RequiredArgsConstructor
@Validated
public class DesignerSettlementController {

    private final DesignerSettlementService settlementService;

    /** 查询分润列表 */
    @Operation(summary = "查询分润列表", description = "管理员分页查询所有分润结算记录")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/settlement/list")
    public Result<PageResult<DesignerSettlementVO>> listSettlements(@Valid @RequestBody DesignerSettlementQueryRequest request) {
        return Result.success(settlementService.querySettlements(request));
    }

    /** 我的分润列表 */
    @Operation(summary = "我的分润列表", description = "设计者分页查询自己的分润结算记录")
    @PreAuthorize("hasAuthority('ROLE_DESIGNER')")
    @PostMapping("/settlement/my/list")
    public Result<PageResult<DesignerSettlementVO>> listMySettlements(
            @Valid @RequestBody DesignerSettlementQueryRequest request,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(settlementService.queryMySettlements(request, loginUser.getId()));
    }

    /** 重试结算 */
    @Operation(summary = "重试结算", description = "管理员重试失败的结算记录")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/settlement/retry/{id}")
    public Result<Void> retrySettlement(@PathVariable Long id) {
        settlementService.retrySettlement(id);
        return Result.success();
    }
}
