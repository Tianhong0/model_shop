package org.majun.backend.service;

import org.majun.backend.vo.statistics.BountyStatisticsVO;
import org.majun.backend.vo.statistics.FinanceStatisticsVO;
import org.majun.backend.vo.statistics.ModelStatisticsVO;
import org.majun.backend.vo.statistics.OrderStatisticsVO;
import org.majun.backend.vo.statistics.StatisticsQuery;
import org.majun.backend.vo.statistics.UserStatisticsVO;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 统计服务接口
 */
public interface StatisticsService {

    /**
     * 订单统计
     */
    OrderStatisticsVO getOrderStatistics(StatisticsQuery query);

    /**
     * 用户统计
     */
    UserStatisticsVO getUserStatistics(StatisticsQuery query);

    /**
     * 模型统计
     */
    ModelStatisticsVO getModelStatistics(StatisticsQuery query);

    /**
     * 财务统计
     */
    FinanceStatisticsVO getFinanceStatistics(StatisticsQuery query);

    /**
     * 悬赏统计
     */
    BountyStatisticsVO getBountyStatistics(StatisticsQuery query);

    /**
     * 导出统计报表
     */
    void exportStatistics(String module, StatisticsQuery query, HttpServletResponse response);
}
