package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.Result;
import org.majun.backend.service.StatisticsService;
import org.majun.backend.vo.statistics.BountyStatisticsVO;
import org.majun.backend.vo.statistics.FinanceStatisticsVO;
import org.majun.backend.vo.statistics.ModelStatisticsVO;
import org.majun.backend.vo.statistics.OrderStatisticsVO;
import org.majun.backend.vo.statistics.StatisticsQuery;
import org.majun.backend.vo.statistics.UserStatisticsVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.majun.backend.annotation.OperationLog;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;

@Tag(name = "Statistics", description = "统计报表接口")
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
/**
 * 统计控制器
 */
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "订单统计")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/orders")
    public Result<OrderStatisticsVO> getOrderStatistics(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        StatisticsQuery query = StatisticsQuery.of(startDate, endDate);
        return Result.success(statisticsService.getOrderStatistics(query));
    }

    @Operation(summary = "用户统计")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/users")
    public Result<UserStatisticsVO> getUserStatistics(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        StatisticsQuery query = StatisticsQuery.of(startDate, endDate);
        return Result.success(statisticsService.getUserStatistics(query));
    }

    @Operation(summary = "模型统计")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/models")
    public Result<ModelStatisticsVO> getModelStatistics(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        StatisticsQuery query = StatisticsQuery.of(startDate, endDate);
        return Result.success(statisticsService.getModelStatistics(query));
    }

    @Operation(summary = "财务统计")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/finance")
    public Result<FinanceStatisticsVO> getFinanceStatistics(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        StatisticsQuery query = StatisticsQuery.of(startDate, endDate);
        return Result.success(statisticsService.getFinanceStatistics(query));
    }

    @Operation(summary = "悬赏统计")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/bounty")
    public Result<BountyStatisticsVO> getBountyStatistics(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        StatisticsQuery query = StatisticsQuery.of(startDate, endDate);
        return Result.success(statisticsService.getBountyStatistics(query));
    }

    @Operation(summary = "导出统计报表")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @OperationLog(type = "EXPORT", module = "统计报表", description = "导出统计报表", targetType = "STATISTICS")
    @GetMapping("/export")
    public void exportStatistics(
            @Parameter(description = "模块: orders/users/models/finance/bounty") @RequestParam String module,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletResponse response
    ) {
        StatisticsQuery query = StatisticsQuery.of(startDate, endDate);
        statisticsService.exportStatistics(module, query, response);
    }
}
