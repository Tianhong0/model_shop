package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.Result;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.DashboardService;
import org.majun.backend.vo.DashboardMessageVO;
import org.majun.backend.vo.DashboardOverviewVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Dashboard", description = "后台仪表盘")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
/**
 * 仪表盘控制器
 */
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "后台仪表盘总览", description = "管理员获取仪表盘核心指标、趋势、待办和最近订单")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/admin/overview")
    public Result<DashboardOverviewVO> getAdminOverview() {
        return Result.success(dashboardService.getAdminOverview());
    }

    @Operation(summary = "后台消息列表", description = "管理员获取右上角消息通知")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/admin/messages")
    public Result<List<DashboardMessageVO>> getAdminMessages(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(dashboardService.getAdminMessages(loginUser.getId()));
    }

    @Operation(summary = "消息全部已读", description = "管理员将当前消息全部标记为已读")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/messages/read-all")
    public Result<Void> markAllMessagesRead(@AuthenticationPrincipal LoginUser loginUser) {
        dashboardService.markAllAdminMessagesRead(loginUser.getId());
        return Result.success();
    }
}
