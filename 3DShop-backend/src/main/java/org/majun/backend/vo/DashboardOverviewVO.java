package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "后台仪表盘总览")
public class DashboardOverviewVO {

    @Schema(description = "核心KPI")
    private KpiVO kpi = new KpiVO();

    @Schema(description = "近7天趋势")
    private List<TrendPointVO> trend7d = new ArrayList<>();

    @Schema(description = "待办统计")
    private TodoVO todo = new TodoVO();

    @Schema(description = "最近订单")
    private List<RecentOrderVO> recentOrders = new ArrayList<>();

    @Schema(description = "生成时间")
    private LocalDateTime generatedAt;

    @Data
    @Schema(description = "核心KPI")
    public static class KpiVO {

        @Schema(description = "用户总数")
        private Long totalUsers;

        @Schema(description = "订单总数")
        private Long totalOrders;

        @Schema(description = "模型总数")
        private Long totalModels;

        @Schema(description = "待办总数")
        private Long totalTodos;

        @Schema(description = "近7天订单金额")
        private BigDecimal orderAmount7d;

        @Schema(description = "用户新增环比(今日vs昨日, %) ")
        private BigDecimal usersTrendPct;

        @Schema(description = "订单新增环比(今日vs昨日, %) ")
        private BigDecimal ordersTrendPct;

        @Schema(description = "模型新增环比(今日vs昨日, %) ")
        private BigDecimal modelsTrendPct;

        @Schema(description = "待办新增环比(今日vs昨日, %) ")
        private BigDecimal todosTrendPct;

        @Schema(description = "近7天金额环比(近7天vs前7天, %) ")
        private BigDecimal orderAmount7dTrendPct;
    }

    @Data
    @Schema(description = "趋势点")
    public static class TrendPointVO {

        @Schema(description = "日期 yyyy-MM-dd")
        private String date;

        @Schema(description = "订单量")
        private Integer orderCount;

        @Schema(description = "订单金额")
        private BigDecimal orderAmount;
    }

    @Data
    @Schema(description = "待办统计")
    public static class TodoVO {

        @Schema(description = "售后待处理")
        private Long afterSalePending;

        @Schema(description = "提现待审核")
        private Long withdrawPending;

        @Schema(description = "注销申请待审核")
        private Long deletionPending;

        @Schema(description = "模型待审核")
        private Long modelReviewPending;

        @Schema(description = "悬赏待审核")
        private Long bountyReviewPending;

        @Schema(description = "管理员注册待审核")
        private Long adminRegisterPending;

        @Schema(description = "设计者申请待审核")
        private Long designerApplyPending;

        @Schema(description = "打印异常")
        private Long printException;

        @Schema(description = "悬赏争议")
        private Long bountyDisputed;

        @Schema(description = "总数")
        private Long total;
    }

    @Data
    @Schema(description = "最近订单")
    public static class RecentOrderVO {

        @Schema(description = "订单ID")
        private Long orderId;

        @Schema(description = "订单号")
        private String orderSn;

        @Schema(description = "用户ID")
        private Long userId;

        @Schema(description = "用户昵称")
        private String userNickname;

        @Schema(description = "订单金额")
        private BigDecimal orderPrice;

        @Schema(description = "订单状态")
        private Integer orderStatus;

        @Schema(description = "创建时间")
        private LocalDateTime createTime;
    }
}
