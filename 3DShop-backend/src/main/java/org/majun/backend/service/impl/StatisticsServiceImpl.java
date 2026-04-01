package org.majun.backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.majun.backend.repository.StatisticsMapper;
import org.majun.backend.service.StatisticsService;
import org.majun.backend.vo.statistics.BountyStatisticsVO;
import org.majun.backend.vo.statistics.FinanceStatisticsVO;
import org.majun.backend.vo.statistics.ModelStatisticsVO;
import org.majun.backend.vo.statistics.OrderStatisticsVO;
import org.majun.backend.vo.statistics.StatisticsQuery;
import org.majun.backend.vo.statistics.UserStatisticsVO;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    private final StatisticsMapper statisticsMapper;

    @Override
    public OrderStatisticsVO getOrderStatistics(StatisticsQuery query) {
        OrderStatisticsVO vo = new OrderStatisticsVO();
        LocalDateTime startTime = query.getStartDateTime();
        LocalDateTime endTime = query.getEndDateTime();

        log.info("查询订单统计，时间范围: {} - {}", startTime, endTime);

        // 汇总数据
        Map<String, Object> summary = statisticsMapper.selectOrderSummary(startTime, endTime);
        log.info("订单汇总数据: {}", summary);
        OrderStatisticsVO.OrderSummary orderSummary = vo.getSummary();
        orderSummary.setTotalOrders(getLong(summary, "totalOrders"));
        orderSummary.setTotalAmount(getBigDecimal(summary, "totalAmount"));
        orderSummary.setAvgOrderAmount(getBigDecimal(summary, "avgOrderAmount"));
        orderSummary.setPaidOrders(getLong(summary, "paidOrders"));
        orderSummary.setCompletedOrders(getLong(summary, "completedOrders"));
        orderSummary.setCanceledOrders(getLong(summary, "canceledOrders"));

        long total = orderSummary.getTotalOrders() != null ? orderSummary.getTotalOrders() : 0L;
        long canceled = orderSummary.getCanceledOrders() != null ? orderSummary.getCanceledOrders() : 0L;
        long completed = orderSummary.getCompletedOrders() != null ? orderSummary.getCompletedOrders() : 0L;
        orderSummary.setCancelRate(calculateRate(canceled, total));
        orderSummary.setCompleteRate(calculateRate(completed, total));

        // 趋势数据
        List<Map<String, Object>> trendList = statisticsMapper.selectOrderTrend(startTime, endTime);
        for (Map<String, Object> item : trendList) {
            OrderStatisticsVO.TrendPoint point = new OrderStatisticsVO.TrendPoint();
            point.setDate(getString(item, "date"));
            point.setOrderCount(getInt(item, "orderCount"));
            point.setOrderAmount(getBigDecimal(item, "orderAmount"));
            point.setPaidUserCount(getInt(item, "paidUserCount"));
            vo.getTrend().add(point);
        }

        // 状态分布
        List<Map<String, Object>> statusList = statisticsMapper.selectOrderStatusDistribution(startTime, endTime);
        long statusTotal = statusList.stream().mapToLong(m -> getLong(m, "count")).sum();
        for (Map<String, Object> item : statusList) {
            OrderStatisticsVO.StatusDistribution dist = new OrderStatisticsVO.StatusDistribution();
            dist.setStatus(getInt(item, "status"));
            dist.setStatusName(getOrderStatusName(dist.getStatus()));
            dist.setCount(getLong(item, "count"));
            dist.setPercentage(calculateRate(dist.getCount(), statusTotal));
            vo.getStatusDistribution().add(dist);
        }

        // 支付渠道分布
        List<Map<String, Object>> channelList = statisticsMapper.selectPaymentChannelDistribution(startTime, endTime);
        long channelTotal = channelList.stream().mapToLong(m -> getLong(m, "count")).sum();
        for (Map<String, Object> item : channelList) {
            OrderStatisticsVO.PaymentChannel channel = new OrderStatisticsVO.PaymentChannel();
            channel.setChannel(getString(item, "channel"));
            channel.setChannelName(getChannelName(channel.getChannel()));
            channel.setCount(getLong(item, "count"));
            channel.setAmount(getBigDecimal(item, "amount"));
            channel.setPercentage(calculateRate(channel.getCount(), channelTotal));
            vo.getPaymentChannels().add(channel);
        }

        // 售后统计
        Map<String, Object> afterSaleStats = statisticsMapper.selectAfterSaleStats(startTime, endTime);
        OrderStatisticsVO.AfterSaleStats afterSale = vo.getAfterSale();
        afterSale.setTotalApplications(getLong(afterSaleStats, "totalApplications"));
        afterSale.setApprovedCount(getLong(afterSaleStats, "approvedCount"));
        afterSale.setRejectedCount(getLong(afterSaleStats, "rejectedCount"));
        afterSale.setPendingCount(getLong(afterSaleStats, "pendingCount"));
        afterSale.setTotalRefundAmount(getBigDecimal(afterSaleStats, "totalRefundAmount"));
        long approved = getLong(afterSaleStats, "approvedCount");
        long totalApps = getLong(afterSaleStats, "totalApplications");
        afterSale.setApprovalRate(calculateRate(approved, totalApps));

        // 评价统计
        Map<String, Object> commentStats = statisticsMapper.selectCommentStats(startTime, endTime);
        OrderStatisticsVO.CommentStats comment = vo.getComment();
        comment.setTotalComments(getLong(commentStats, "totalComments"));
        comment.setAvgModelScore(getBigDecimal(commentStats, "avgModelScore"));
        comment.setAvgPrintScore(getBigDecimal(commentStats, "avgPrintScore"));
        comment.setAvgServiceScore(getBigDecimal(commentStats, "avgServiceScore"));
        if (comment.getAvgModelScore() != null && comment.getAvgPrintScore() != null && comment.getAvgServiceScore() != null) {
            BigDecimal overall = comment.getAvgModelScore()
                .add(comment.getAvgPrintScore())
                .add(comment.getAvgServiceScore())
                .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
            comment.setAvgOverallScore(overall);
        }

        return vo;
    }

    @Override
    public UserStatisticsVO getUserStatistics(StatisticsQuery query) {
        UserStatisticsVO vo = new UserStatisticsVO();
        LocalDateTime startTime = query.getStartDateTime();
        LocalDateTime endTime = query.getEndDateTime();

        // 汇总数据
        Map<String, Object> summary = statisticsMapper.selectUserSummary(startTime, endTime);
        UserStatisticsVO.UserSummary userSummary = vo.getSummary();
        userSummary.setTotalUsers(getLong(summary, "totalUsers"));
        userSummary.setNewUsers(getLong(summary, "newUsers"));
        userSummary.setNormalUsers(getLong(summary, "normalUsers"));
        userSummary.setDisabledUsers(getLong(summary, "disabledUsers"));
        userSummary.setMaleUsers(getLong(summary, "maleUsers"));
        userSummary.setFemaleUsers(getLong(summary, "femaleUsers"));

        // 增长趋势
        List<Map<String, Object>> trendList = statisticsMapper.selectUserGrowthTrend(startTime, endTime);
        long cumulative = userSummary.getTotalUsers() != null ? userSummary.getTotalUsers() : 0L;
        for (int i = trendList.size() - 1; i >= 0; i--) {
            Map<String, Object> item = trendList.get(i);
            UserStatisticsVO.GrowthTrend trend = new UserStatisticsVO.GrowthTrend();
            trend.setDate(getString(item, "date"));
            trend.setNewUserCount(getInt(item, "newUserCount"));
            cumulative -= trend.getNewUserCount();
            trend.setTotalUserCount(cumulative);
            vo.getGrowthTrend().add(0, trend);
        }

        // 角色分布
        List<Map<String, Object>> roleList = statisticsMapper.selectRoleDistribution();
        long roleTotal = roleList.stream().mapToLong(m -> getLong(m, "userCount")).sum();
        for (Map<String, Object> item : roleList) {
            UserStatisticsVO.RoleDistribution dist = new UserStatisticsVO.RoleDistribution();
            dist.setRoleId(getLong(item, "roleId"));
            dist.setRoleName(getString(item, "roleName"));
            dist.setUserCount(getLong(item, "userCount"));
            dist.setPercentage(calculateRate(dist.getUserCount(), roleTotal));
            vo.getRoleDistribution().add(dist);
        }

        // 设计者统计
        Map<String, Object> designerStats = statisticsMapper.selectDesignerStats();
        UserStatisticsVO.DesignerStats designer = vo.getDesignerStats();
        designer.setTotalDesigners(getLong(designerStats, "totalDesigners"));
        designer.setPendingApplications(getLong(designerStats, "pendingApplications"));
        designer.setApprovedApplications(getLong(designerStats, "approvedApplications"));
        designer.setRejectedApplications(getLong(designerStats, "rejectedApplications"));
        designer.setAvgCreditScore(getBigDecimal(designerStats, "avgCreditScore"));
        designer.setAvgRating(getBigDecimal(designerStats, "avgRating"));

        return vo;
    }

    @Override
    public ModelStatisticsVO getModelStatistics(StatisticsQuery query) {
        ModelStatisticsVO vo = new ModelStatisticsVO();
        LocalDateTime startTime = query.getStartDateTime();
        LocalDateTime endTime = query.getEndDateTime();

        // 汇总数据
        Map<String, Object> summary = statisticsMapper.selectModelSummary(startTime, endTime);
        ModelStatisticsVO.ModelSummary modelSummary = vo.getSummary();
        modelSummary.setTotalModels(getLong(summary, "totalModels"));
        modelSummary.setNewModels(getLong(summary, "newModels"));
        modelSummary.setActiveModels(getLong(summary, "activeModels"));
        modelSummary.setInactiveModels(getLong(summary, "inactiveModels"));
        modelSummary.setPendingModels(getLong(summary, "pendingModels"));
        modelSummary.setAvgPrice(getBigDecimal(summary, "avgPrice"));
        modelSummary.setTotalFavorites(statisticsMapper.selectTotalFavorites());

        // 趋势数据
        List<Map<String, Object>> trendList = statisticsMapper.selectModelTrend(startTime, endTime);
        for (Map<String, Object> item : trendList) {
            ModelStatisticsVO.ModelTrend trend = new ModelStatisticsVO.ModelTrend();
            trend.setDate(getString(item, "date"));
            trend.setNewModelCount(getInt(item, "newModelCount"));
            vo.getTrend().add(trend);
        }

        // 分类分布
        List<Map<String, Object>> categoryList = statisticsMapper.selectCategoryDistribution();
        long categoryTotal = categoryList.stream().mapToLong(m -> getLong(m, "modelCount")).sum();
        for (Map<String, Object> item : categoryList) {
            ModelStatisticsVO.CategoryDistribution dist = new ModelStatisticsVO.CategoryDistribution();
            dist.setCategoryId(getLong(item, "categoryId"));
            dist.setCategoryName(getString(item, "categoryName"));
            dist.setModelCount(getLong(item, "modelCount"));
            dist.setPercentage(calculateRate(dist.getModelCount(), categoryTotal));
            vo.getCategoryDistribution().add(dist);
        }

        // 状态分布
        List<Map<String, Object>> statusList = statisticsMapper.selectModelStatusDistribution();
        for (Map<String, Object> item : statusList) {
            ModelStatisticsVO.StatusStats stats = new ModelStatisticsVO.StatusStats();
            stats.setStatus(getInt(item, "status"));
            stats.setStatusName(getModelStatusName(stats.getStatus()));
            stats.setCount(getLong(item, "count"));
            vo.getStatusDistribution().add(stats);
        }

        // 热门模型
        List<Map<String, Object>> topModels = statisticsMapper.selectTopModels();
        for (Map<String, Object> item : topModels) {
            ModelStatisticsVO.TopModel model = new ModelStatisticsVO.TopModel();
            model.setModelId(getLong(item, "modelId"));
            model.setModelName(getString(item, "modelName"));
            model.setDesignerName(getString(item, "designerName"));
            model.setFavoriteCount(getLong(item, "favoriteCount"));
            model.setPrice(getBigDecimal(item, "price"));
            vo.getTopModels().add(model);
        }

        return vo;
    }

    @Override
    public FinanceStatisticsVO getFinanceStatistics(StatisticsQuery query) {
        FinanceStatisticsVO vo = new FinanceStatisticsVO();
        LocalDateTime startTime = query.getStartDateTime();
        LocalDateTime endTime = query.getEndDateTime();

        // 钱包汇总
        Map<String, Object> walletSummary = statisticsMapper.selectWalletSummary();
        FinanceStatisticsVO.WalletSummary wallet = vo.getWalletSummary();
        wallet.setTotalAvailableBalance(getBigDecimal(walletSummary, "totalAvailableBalance"));
        wallet.setTotalFrozenBalance(getBigDecimal(walletSummary, "totalFrozenBalance"));
        wallet.setTotalAccounts(getLong(walletSummary, "totalAccounts"));
        wallet.setActiveAccounts(getLong(walletSummary, "activeAccounts"));
        wallet.setFrozenAccounts(getLong(walletSummary, "frozenAccounts"));

        // 流水统计
        FinanceStatisticsVO.LedgerStats ledger = vo.getLedgerStats();
        List<Map<String, Object>> ledgerTrend = statisticsMapper.selectLedgerTrend(startTime, endTime);
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        for (Map<String, Object> item : ledgerTrend) {
            totalIncome = totalIncome.add(getBigDecimal(item, "income"));
            totalExpense = totalExpense.add(getBigDecimal(item, "expense"));

            FinanceStatisticsVO.LedgerTrend trend = new FinanceStatisticsVO.LedgerTrend();
            trend.setDate(getString(item, "date"));
            trend.setIncome(getBigDecimal(item, "income"));
            trend.setExpense(getBigDecimal(item, "expense"));
            vo.getLedgerTrend().add(trend);
        }
        ledger.setTotalIncome(totalIncome);
        ledger.setTotalExpense(totalExpense);
        ledger.setTotalTransactions((long) ledgerTrend.size());

        // 提现统计
        Map<String, Object> withdrawStats = statisticsMapper.selectWithdrawStats(startTime, endTime);
        FinanceStatisticsVO.WithdrawStats withdraw = vo.getWithdrawStats();
        withdraw.setTotalApplications(getLong(withdrawStats, "totalApplications"));
        withdraw.setTotalAppliedAmount(getBigDecimal(withdrawStats, "totalAppliedAmount"));
        withdraw.setTotalPaidAmount(getBigDecimal(withdrawStats, "totalPaidAmount"));
        withdraw.setPendingCount(getLong(withdrawStats, "pendingCount"));
        withdraw.setApprovedCount(getLong(withdrawStats, "approvedCount"));
        withdraw.setRejectedCount(getLong(withdrawStats, "rejectedCount"));
        withdraw.setPaidCount(getLong(withdrawStats, "paidCount"));
        long approved = getLong(withdrawStats, "approvedCount") + getLong(withdrawStats, "paidCount");
        long totalApps = getLong(withdrawStats, "totalApplications");
        withdraw.setApprovalRate(calculateRate(approved, totalApps));

        // 积分统计
        Map<String, Object> pointStats = statisticsMapper.selectPointStats();
        FinanceStatisticsVO.PointStats point = vo.getPointStats();
        point.setTotalAvailablePoints(getLong(pointStats, "totalAvailablePoints"));
        point.setTotalEarned(getLong(pointStats, "totalEarned"));
        point.setTotalSpent(getLong(pointStats, "totalSpent"));
        point.setTotalAccounts(getLong(pointStats, "totalAccounts"));

        return vo;
    }

    @Override
    public BountyStatisticsVO getBountyStatistics(StatisticsQuery query) {
        BountyStatisticsVO vo = new BountyStatisticsVO();
        LocalDateTime startTime = query.getStartDateTime();
        LocalDateTime endTime = query.getEndDateTime();

        // 汇总数据
        Map<String, Object> summary = statisticsMapper.selectBountySummary(startTime, endTime);
        BountyStatisticsVO.TaskSummary taskSummary = vo.getTaskSummary();
        taskSummary.setTotalTasks(getLong(summary, "totalTasks"));
        taskSummary.setNewTasks(getLong(summary, "newTasks"));
        taskSummary.setCompletedTasks(getLong(summary, "completedTasks"));
        taskSummary.setOngoingTasks(getLong(summary, "ongoingTasks"));
        taskSummary.setClosedTasks(getLong(summary, "closedTasks"));
        taskSummary.setTotalBudgetAmount(getBigDecimal(summary, "totalBudgetAmount"));
        taskSummary.setTotalFinalAmount(getBigDecimal(summary, "totalFinalAmount"));
        if (taskSummary.getCompletedTasks() != null && taskSummary.getCompletedTasks() > 0) {
            taskSummary.setAvgFinalAmount(taskSummary.getTotalFinalAmount()
                .divide(BigDecimal.valueOf(taskSummary.getCompletedTasks()), 2, RoundingMode.HALF_UP));
        }

        // 趋势数据
        List<Map<String, Object>> trendList = statisticsMapper.selectBountyTrend(startTime, endTime);
        for (Map<String, Object> item : trendList) {
            BountyStatisticsVO.TaskTrend trend = new BountyStatisticsVO.TaskTrend();
            trend.setDate(getString(item, "date"));
            trend.setNewTaskCount(getInt(item, "newTaskCount"));
            trend.setCompletedTaskCount(getInt(item, "completedTaskCount"));
            trend.setFinalAmount(getBigDecimal(item, "finalAmount"));
            vo.getTaskTrend().add(trend);
        }

        // 状态分布
        List<Map<String, Object>> statusList = statisticsMapper.selectBountyStatusDistribution();
        long statusTotal = statusList.stream().mapToLong(m -> getLong(m, "count")).sum();
        for (Map<String, Object> item : statusList) {
            BountyStatisticsVO.StatusDistribution dist = new BountyStatisticsVO.StatusDistribution();
            dist.setStatus(getInt(item, "status"));
            dist.setStatusName(getBountyStatusName(dist.getStatus()));
            dist.setCount(getLong(item, "count"));
            dist.setPercentage(calculateRate(dist.getCount(), statusTotal));
            vo.getStatusDistribution().add(dist);
        }

        // 评分统计
        Map<String, Object> ratingStats = statisticsMapper.selectRatingStats(startTime, endTime);
        BountyStatisticsVO.RatingStats rating = vo.getRatingStats();
        rating.setTotalRatings(getLong(ratingStats, "totalRatings"));
        rating.setAvgRating(getBigDecimal(ratingStats, "avgRating"));
        rating.setFiveStarCount(getLong(ratingStats, "fiveStarCount"));
        rating.setFourStarCount(getLong(ratingStats, "fourStarCount"));
        rating.setThreeStarCount(getLong(ratingStats, "threeStarCount"));
        rating.setTwoStarCount(getLong(ratingStats, "twoStarCount"));
        rating.setOneStarCount(getLong(ratingStats, "oneStarCount"));
        rating.setTotalAppeals(statisticsMapper.selectTotalAppeals(startTime, endTime));

        // 热门设计师
        List<Map<String, Object>> topDesigners = statisticsMapper.selectTopDesigners();
        for (Map<String, Object> item : topDesigners) {
            BountyStatisticsVO.TopDesigner designer = new BountyStatisticsVO.TopDesigner();
            designer.setUserId(getLong(item, "userId"));
            designer.setNickname(getString(item, "nickname"));
            designer.setCompletedTasks(getLong(item, "completedTasks"));
            designer.setTotalIncome(getBigDecimal(item, "totalIncome"));
            designer.setAvgRating(getBigDecimal(item, "avgRating"));
            designer.setCreditScore(getInt(item, "creditScore"));
            vo.getTopDesigners().add(designer);
        }

        return vo;
    }

    @Override
    public void exportStatistics(String module, StatisticsQuery query, HttpServletResponse response) {
        try {
            String filename = module + "_statistics_" + query.getStartDate() + "_" + query.getEndDate() + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8));

            try (SXSSFWorkbook workbook = new SXSSFWorkbook(1000)) {
                switch (module) {
                    case "orders" -> exportOrderStatistics(workbook, query);
                    case "users" -> exportUserStatistics(workbook, query);
                    case "models" -> exportModelStatistics(workbook, query);
                    case "finance" -> exportFinanceStatistics(workbook, query);
                    case "bounty" -> exportBountyStatistics(workbook, query);
                    default -> throw new IllegalArgumentException("Unknown module: " + module);
                }
                workbook.write(response.getOutputStream());
            }
        } catch (IOException e) {
            log.error("导出统计报表失败", e);
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    private void exportOrderStatistics(Workbook workbook, StatisticsQuery query) {
        Sheet sheet = workbook.createSheet("订单统计");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("日期");
        header.createCell(1).setCellValue("订单量");
        header.createCell(2).setCellValue("订单金额");
        header.createCell(3).setCellValue("付费用户数");

        OrderStatisticsVO stats = getOrderStatistics(query);
        int rowNum = 1;
        for (OrderStatisticsVO.TrendPoint point : stats.getTrend()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(point.getDate());
            row.createCell(1).setCellValue(point.getOrderCount() != null ? point.getOrderCount() : 0);
            row.createCell(2).setCellValue(point.getOrderAmount() != null ? point.getOrderAmount().toString() : "0");
            row.createCell(3).setCellValue(point.getPaidUserCount() != null ? point.getPaidUserCount() : 0);
        }
    }

    private void exportUserStatistics(Workbook workbook, StatisticsQuery query) {
        Sheet sheet = workbook.createSheet("用户统计");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("日期");
        header.createCell(1).setCellValue("新增用户数");
        header.createCell(2).setCellValue("累计用户数");

        UserStatisticsVO stats = getUserStatistics(query);
        int rowNum = 1;
        for (UserStatisticsVO.GrowthTrend point : stats.getGrowthTrend()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(point.getDate());
            row.createCell(1).setCellValue(point.getNewUserCount() != null ? point.getNewUserCount() : 0);
            row.createCell(2).setCellValue(point.getTotalUserCount() != null ? point.getTotalUserCount() : 0);
        }
    }

    private void exportModelStatistics(Workbook workbook, StatisticsQuery query) {
        Sheet sheet = workbook.createSheet("模型统计");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("日期");
        header.createCell(1).setCellValue("新增模型数");

        ModelStatisticsVO stats = getModelStatistics(query);
        int rowNum = 1;
        for (ModelStatisticsVO.ModelTrend point : stats.getTrend()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(point.getDate());
            row.createCell(1).setCellValue(point.getNewModelCount() != null ? point.getNewModelCount() : 0);
        }
    }

    private void exportFinanceStatistics(Workbook workbook, StatisticsQuery query) {
        Sheet sheet = workbook.createSheet("财务统计");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("日期");
        header.createCell(1).setCellValue("收入");
        header.createCell(2).setCellValue("支出");

        FinanceStatisticsVO stats = getFinanceStatistics(query);
        int rowNum = 1;
        for (FinanceStatisticsVO.LedgerTrend point : stats.getLedgerTrend()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(point.getDate());
            row.createCell(1).setCellValue(point.getIncome() != null ? point.getIncome().toString() : "0");
            row.createCell(2).setCellValue(point.getExpense() != null ? point.getExpense().toString() : "0");
        }
    }

    private void exportBountyStatistics(Workbook workbook, StatisticsQuery query) {
        Sheet sheet = workbook.createSheet("悬赏统计");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("日期");
        header.createCell(1).setCellValue("新增任务数");
        header.createCell(2).setCellValue("完成任务数");
        header.createCell(3).setCellValue("成交金额");

        BountyStatisticsVO stats = getBountyStatistics(query);
        int rowNum = 1;
        for (BountyStatisticsVO.TaskTrend point : stats.getTaskTrend()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(point.getDate());
            row.createCell(1).setCellValue(point.getNewTaskCount() != null ? point.getNewTaskCount() : 0);
            row.createCell(2).setCellValue(point.getCompletedTaskCount() != null ? point.getCompletedTaskCount() : 0);
            row.createCell(3).setCellValue(point.getFinalAmount() != null ? point.getFinalAmount().toString() : "0");
        }
    }

    // ==================== 辅助方法 ====================

    private Object getValueIgnoreCase(Map<String, Object> map, String key) {
        if (map == null) return null;
        // 先尝试精确匹配
        Object value = map.get(key);
        if (value != null) return value;
        // 再尝试大小写不敏感匹配
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object value = getValueIgnoreCase(map, key);
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }

    private Integer getInt(Map<String, Object> map, String key) {
        Object value = getValueIgnoreCase(map, key);
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        return 0;
    }

    private BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object value = getValueIgnoreCase(map, key);
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        return BigDecimal.ZERO;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = getValueIgnoreCase(map, key);
        return value != null ? value.toString() : "";
    }

    private BigDecimal calculateRate(long part, long total) {
        if (total == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(part)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private String getOrderStatusName(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "生产中";
            case 2 -> "待发货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            default -> "未知";
        };
    }

    private String getModelStatusName(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "审核中";
            case 1 -> "上架";
            case 2 -> "下架";
            default -> "未知";
        };
    }

    private String getChannelName(String channel) {
        if (channel == null) return "未知";
        return switch (channel.toUpperCase()) {
            case "ALIPAY_APP" -> "支付宝APP";
            case "ALIPAY_WAP" -> "支付宝WAP";
            case "WALLET" -> "钱包余额";
            default -> channel;
        };
    }

    private String getBountyStatusName(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待支付托管";
            case 1 -> "招募中";
            case 2 -> "已选标";
            case 3 -> "交付中";
            case 4 -> "待验收";
            case 5 -> "已完成";
            case 6 -> "已关闭";
            case 7 -> "争议中";
            default -> "未知";
        };
    }
}
