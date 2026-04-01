package org.majun.backend.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface StatisticsMapper {

    // ==================== 订单统计 ====================

    @Select("""
        SELECT
            DATE(create_time) as date,
            COUNT(*) as orderCount,
            SUM(order_price) as orderAmount,
            COUNT(DISTINCT user_id) as paidUserCount
        FROM sys_order
        WHERE create_time >= #{startTime} AND create_time < #{endTime}
          AND is_delete = 0
        GROUP BY DATE(create_time)
        ORDER BY date
    """)
    List<Map<String, Object>> selectOrderTrend(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Select("""
        SELECT
            order_status as status,
            COUNT(*) as count,
            SUM(order_price) as amount
        FROM sys_order
        WHERE create_time >= #{startTime} AND create_time < #{endTime}
          AND is_delete = 0
        GROUP BY order_status
    """)
    List<Map<String, Object>> selectOrderStatusDistribution(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Select("""
        SELECT
            pay_channel as channel,
            COUNT(*) as count,
            SUM(total_amount) as amount
        FROM sys_order_pay_batch
        WHERE pay_status = 1
          AND pay_time >= #{startTime} AND pay_time < #{endTime}
          AND is_delete = 0
        GROUP BY pay_channel
    """)
    List<Map<String, Object>> selectPaymentChannelDistribution(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Select("""
        SELECT
            COUNT(*) as totalApplications,
            SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as approvedCount,
            SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) as rejectedCount,
            SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) as pendingCount,
            COALESCE(SUM(approved_amount), 0) as totalRefundAmount
        FROM sys_order_after_sale
        WHERE create_time >= #{startTime} AND create_time < #{endTime}
          AND is_delete = 0
    """)
    Map<String, Object> selectAfterSaleStats(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Select("""
        SELECT
            COUNT(*) as totalComments,
            AVG(model_score) as avgModelScore,
            AVG(print_score) as avgPrintScore,
            AVG(service_score) as avgServiceScore
        FROM sys_order_comment
        WHERE create_time >= #{startTime} AND create_time < #{endTime}
          AND status = 1
    """)
    Map<String, Object> selectCommentStats(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    // ==================== 用户统计 ====================

    @Select("""
        SELECT
            DATE(create_time) as date,
            COUNT(*) as newUserCount
        FROM sys_user
        WHERE create_time >= #{startTime} AND create_time < #{endTime}
          AND is_delete = 0
        GROUP BY DATE(create_time)
        ORDER BY date
    """)
    List<Map<String, Object>> selectUserGrowthTrend(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Select("""
        SELECT
            r.id as roleId,
            r.role_name as roleName,
            COUNT(ur.user_id) as userCount
        FROM sys_role r
        LEFT JOIN sys_user_role ur ON r.id = ur.role_id
        LEFT JOIN sys_user u ON ur.user_id = u.id AND u.is_delete = 0
        WHERE r.is_delete = 0
        GROUP BY r.id, r.role_name
    """)
    List<Map<String, Object>> selectRoleDistribution();

    @Select("""
        SELECT
            COUNT(*) as totalDesigners,
            SUM(CASE WHEN da.status = 'pending' THEN 1 ELSE 0 END) as pendingApplications,
            SUM(CASE WHEN da.status = 'approved' THEN 1 ELSE 0 END) as approvedApplications,
            SUM(CASE WHEN da.status = 'rejected' THEN 1 ELSE 0 END) as rejectedApplications,
            AVG(dr.reputation_score) as avgCreditScore,
            AVG(dr.avg_score) as avgRating
        FROM sys_user_role ur
        JOIN sys_role r ON ur.role_id = r.id AND r.role_name = 'ROLE_DESIGNER'
        JOIN sys_user u ON ur.user_id = u.id AND u.is_delete = 0
        LEFT JOIN designer_apply_request da ON da.user_id = u.id
        LEFT JOIN designer_reputation dr ON dr.designer_id = u.id
    """)
    Map<String, Object> selectDesignerStats();

    // ==================== 模型统计 ====================

    @Select("""
        SELECT
            DATE(create_time) as date,
            COUNT(*) as newModelCount
        FROM sys_model
        WHERE create_time >= #{startTime} AND create_time < #{endTime}
          AND is_delete = 0
        GROUP BY DATE(create_time)
        ORDER BY date
    """)
    List<Map<String, Object>> selectModelTrend(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Select("""
        SELECT
            c.id as categoryId,
            c.category_name as categoryName,
            COUNT(m.id) as modelCount
        FROM sys_model_category c
        LEFT JOIN sys_model m ON c.id = m.category_id AND m.is_delete = 0
        WHERE c.is_delete = 0
        GROUP BY c.id, c.category_name
        ORDER BY modelCount DESC
    """)
    List<Map<String, Object>> selectCategoryDistribution();

    @Select("""
        SELECT
            status,
            COUNT(*) as count
        FROM sys_model
        WHERE is_delete = 0
        GROUP BY status
    """)
    List<Map<String, Object>> selectModelStatusDistribution();

    @Select("""
        SELECT
            m.id as modelId,
            m.model_name as modelName,
            u.nickname as designerName,
            COUNT(f.id) as favoriteCount,
            m.base_price as price
        FROM sys_model m
        LEFT JOIN sys_user u ON m.designer_id = u.id
        LEFT JOIN sys_model_favorite f ON m.id = f.model_id
        WHERE m.is_delete = 0 AND m.status = 1
        GROUP BY m.id, m.model_name, u.nickname, m.base_price
        ORDER BY favoriteCount DESC
        LIMIT 10
    """)
    List<Map<String, Object>> selectTopModels();

    // ==================== 财务统计 ====================

    @Select("""
        SELECT
            SUM(available_balance) as totalAvailableBalance,
            SUM(frozen_balance) as totalFrozenBalance,
            COUNT(*) as totalAccounts,
            SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as activeAccounts,
            SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) as frozenAccounts
        FROM wallet_account
        WHERE is_delete = 0
    """)
    Map<String, Object> selectWalletSummary();

    @Select("""
        SELECT
            DATE(create_time) as date,
            SUM(CASE WHEN direction = 1 THEN amount ELSE 0 END) as income,
            SUM(CASE WHEN direction = 2 THEN amount ELSE 0 END) as expense
        FROM wallet_ledger
        WHERE create_time >= #{startTime} AND create_time < #{endTime}
        GROUP BY DATE(create_time)
        ORDER BY date
    """)
    List<Map<String, Object>> selectLedgerTrend(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Select("""
        SELECT
            COUNT(*) as totalApplications,
            SUM(amount) as totalAppliedAmount,
            SUM(CASE WHEN status = 3 THEN amount ELSE 0 END) as totalPaidAmount,
            SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) as pendingCount,
            SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as approvedCount,
            SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) as rejectedCount,
            SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) as paidCount
        FROM wallet_withdraw
        WHERE create_time >= #{startTime} AND create_time < #{endTime}
          AND is_delete = 0
    """)
    Map<String, Object> selectWithdrawStats(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Select("""
        SELECT
            SUM(available_points) as totalAvailablePoints,
            SUM(total_earned) as totalEarned,
            SUM(total_spent) as totalSpent,
            COUNT(*) as totalAccounts
        FROM point_account
        WHERE is_delete = 0
    """)
    Map<String, Object> selectPointStats();

    // ==================== 悬赏统计 ====================

    @Select("""
        SELECT
            DATE(create_time) as date,
            COUNT(*) as newTaskCount,
            SUM(CASE WHEN status = 5 THEN 1 ELSE 0 END) as completedTaskCount,
            SUM(CASE WHEN status = 5 THEN final_amount ELSE 0 END) as finalAmount
        FROM bounty_task
        WHERE create_time >= #{startTime} AND create_time < #{endTime}
          AND is_delete = 0
        GROUP BY DATE(create_time)
        ORDER BY date
    """)
    List<Map<String, Object>> selectBountyTrend(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Select("""
        SELECT
            status,
            COUNT(*) as count
        FROM bounty_task
        WHERE is_delete = 0
        GROUP BY status
    """)
    List<Map<String, Object>> selectBountyStatusDistribution();

    @Select("""
        SELECT
            COUNT(*) as totalRatings,
            AVG(score) as avgRating,
            SUM(CASE WHEN score = 5 THEN 1 ELSE 0 END) as fiveStarCount,
            SUM(CASE WHEN score = 4 THEN 1 ELSE 0 END) as fourStarCount,
            SUM(CASE WHEN score = 3 THEN 1 ELSE 0 END) as threeStarCount,
            SUM(CASE WHEN score = 2 THEN 1 ELSE 0 END) as twoStarCount,
            SUM(CASE WHEN score = 1 THEN 1 ELSE 0 END) as oneStarCount
        FROM bounty_rating
        WHERE create_time >= #{startTime} AND create_time < #{endTime}
          AND is_delete = 0
    """)
    Map<String, Object> selectRatingStats(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Select("""
        SELECT
            u.id as userId,
            u.nickname as nickname,
            COUNT(CASE WHEN bt.status = 5 THEN 1 END) as completedTasks,
            SUM(CASE WHEN bt.status = 5 THEN bt.final_amount ELSE 0 END) as totalIncome,
            AVG(br.score) as avgRating,
            COALESCE(dr.reputation_score, 100) as creditScore
        FROM sys_user u
        JOIN sys_user_role ur ON u.id = ur.user_id
        JOIN sys_role r ON ur.role_id = r.id AND r.role_name = 'ROLE_DESIGNER'
        LEFT JOIN bounty_task bt ON bt.winner_designer_id = u.id AND bt.is_delete = 0
        LEFT JOIN bounty_rating br ON br.designer_id = u.id
        LEFT JOIN designer_reputation dr ON dr.designer_id = u.id
        WHERE u.is_delete = 0
        GROUP BY u.id, u.nickname, dr.reputation_score
        ORDER BY completedTasks DESC, totalIncome DESC
        LIMIT 10
    """)
    List<Map<String, Object>> selectTopDesigners();

    // ==================== 综合统计 ====================

    @Select("""
        SELECT
            COUNT(*) as totalOrders,
            COALESCE(SUM(order_price), 0) as totalAmount,
            COALESCE(AVG(order_price), 0) as avgOrderAmount,
            SUM(CASE WHEN order_status != 0 THEN 1 ELSE 0 END) as paidOrders,
            SUM(CASE WHEN order_status = 3 THEN 1 ELSE 0 END) as completedOrders,
            SUM(CASE WHEN order_status = 4 THEN 1 ELSE 0 END) as canceledOrders
        FROM sys_order
        WHERE create_time >= #{startTime} AND create_time < #{endTime}
          AND is_delete = 0
    """)
    Map<String, Object> selectOrderSummary(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Select("""
        SELECT
            COUNT(*) as totalUsers,
            SUM(CASE WHEN create_time >= #{startTime} AND create_time < #{endTime} THEN 1 ELSE 0 END) as newUsers,
            SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as normalUsers,
            SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) as disabledUsers,
            SUM(CASE WHEN sex = 1 THEN 1 ELSE 0 END) as maleUsers,
            SUM(CASE WHEN sex = 0 THEN 1 ELSE 0 END) as femaleUsers
        FROM sys_user
        WHERE is_delete = 0
    """)
    Map<String, Object> selectUserSummary(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Select("""
        SELECT
            COUNT(*) as totalModels,
            SUM(CASE WHEN create_time >= #{startTime} AND create_time < #{endTime} THEN 1 ELSE 0 END) as newModels,
            SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as activeModels,
            SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) as inactiveModels,
            SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) as pendingModels,
            COALESCE(AVG(base_price), 0) as avgPrice
        FROM sys_model
        WHERE is_delete = 0
    """)
    Map<String, Object> selectModelSummary(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Select("SELECT COUNT(*) FROM sys_model_favorite")
    Long selectTotalFavorites();

    @Select("""
        SELECT
            COUNT(*) as totalTasks,
            SUM(CASE WHEN create_time >= #{startTime} AND create_time < #{endTime} THEN 1 ELSE 0 END) as newTasks,
            SUM(CASE WHEN status = 5 THEN 1 ELSE 0 END) as completedTasks,
            SUM(CASE WHEN status IN (1,2,3,4) THEN 1 ELSE 0 END) as ongoingTasks,
            SUM(CASE WHEN status = 6 THEN 1 ELSE 0 END) as closedTasks,
            COALESCE(SUM(budget_amount), 0) as totalBudgetAmount,
            COALESCE(SUM(CASE WHEN status = 5 THEN final_amount ELSE 0 END), 0) as totalFinalAmount
        FROM bounty_task
        WHERE is_delete = 0
    """)
    Map<String, Object> selectBountySummary(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Select("SELECT COUNT(*) FROM bounty_rating WHERE create_time >= #{startTime} AND create_time < #{endTime}")
    Long selectTotalAppeals(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
