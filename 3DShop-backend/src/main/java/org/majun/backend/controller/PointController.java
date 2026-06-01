package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.Result;
import org.majun.backend.dto.PointLedgerQueryRequest;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.PointService;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.PointAccountVO;
import org.majun.backend.vo.PointLedgerVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Point", description = "积分接口")
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
/**
 * 积分控制器
 */
public class PointController {

    private final PointService pointService;

    /** 积分账户概览 */
    @GetMapping("/account")
    @Operation(summary = "积分账户概览")
    public Result<PointAccountVO> getAccount(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(pointService.getAccount(loginUser.getId()));
    }

    /** 积分流水分页 */
    @PostMapping("/ledger/page")
    @Operation(summary = "积分流水分页")
    public Result<PageResult<PointLedgerVO>> pageLedger(@AuthenticationPrincipal LoginUser loginUser,
                                                        @RequestBody(required = false) PointLedgerQueryRequest request) {
        PointLedgerQueryRequest req = request == null ? new PointLedgerQueryRequest() : request;
        return Result.success(pointService.pageLedger(req, loginUser.getId()));
    }
}
