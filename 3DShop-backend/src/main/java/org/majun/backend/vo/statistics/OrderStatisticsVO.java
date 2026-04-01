package org.majun.backend.vo.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "订单统计")
public class OrderStatisticsVO {

    @Schema(description = "汇总数据")
    private OrderSummary summary = new OrderSummary();

    @Schema(description = "趋势数据")
    private List<TrendPoint> trend = new ArrayList<>();

    @Schema(description = "状态分布")
    private List<StatusDistribution> statusDistribution = new ArrayList<>();

    @Schema(description = "支付渠道分布")
    private List<PaymentChannel> paymentChannels = new ArrayList<>();

    @Schema(description = "售后统计")
    private AfterSaleStats afterSale = new AfterSaleStats();

    @Schema(description = "评价统计")
    private CommentStats comment = new CommentStats();

    @Data
    @Schema(description = "订单汇总")
    public static class OrderSummary {
        @Schema(description = "订单总数")
        private Long totalOrders;

        @Schema(description = "销售总额")
        private BigDecimal totalAmount;

        @Schema(description = "平均客单价")
        private BigDecimal avgOrderAmount;

        @Schema(description = "已支付订单")
        private Long paidOrders;

        @Schema(description = "已完成订单")
        private Long completedOrders;

        @Schema(description = "已取消订单")
        private Long canceledOrders;

        @Schema(description = "取消率(%)")
        private BigDecimal cancelRate;

        @Schema(description = "完成率(%)")
        private BigDecimal completeRate;
    }

    @Data
    @Schema(description = "趋势数据点")
    public static class TrendPoint {
        @Schema(description = "日期")
        private String date;

        @Schema(description = "订单量")
        private Integer orderCount;

        @Schema(description = "订单金额")
        private BigDecimal orderAmount;

        @Schema(description = "新用户数")
        private Integer newUsers;

        @Schema(description = "付费用户数")
        private Integer paidUserCount;
    }

    @Data
    @Schema(description = "状态分布")
    public static class StatusDistribution {
        @Schema(description = "状态码")
        private Integer status;

        @Schema(description = "状态名称")
        private String statusName;

        @Schema(description = "数量")
        private Long count;

        @Schema(description = "占比(%)")
        private BigDecimal percentage;
    }

    @Data
    @Schema(description = "支付渠道")
    public static class PaymentChannel {
        @Schema(description = "渠道")
        private String channel;

        @Schema(description = "渠道名称")
        private String channelName;

        @Schema(description = "数量")
        private Long count;

        @Schema(description = "金额")
        private BigDecimal amount;

        @Schema(description = "占比(%)")
        private BigDecimal percentage;
    }

    @Data
    @Schema(description = "售后统计")
    public static class AfterSaleStats {
        @Schema(description = "申请总数")
        private Long totalApplications;

        @Schema(description = "已通过")
        private Long approvedCount;

        @Schema(description = "已拒绝")
        private Long rejectedCount;

        @Schema(description = "待处理")
        private Long pendingCount;

        @Schema(description = "通过率(%)")
        private BigDecimal approvalRate;

        @Schema(description = "退款总金额")
        private BigDecimal totalRefundAmount;
    }

    @Data
    @Schema(description = "评价统计")
    public static class CommentStats {
        @Schema(description = "评价总数")
        private Long totalComments;

        @Schema(description = "平均模型评分")
        private BigDecimal avgModelScore;

        @Schema(description = "平均打印评分")
        private BigDecimal avgPrintScore;

        @Schema(description = "平均服务评分")
        private BigDecimal avgServiceScore;

        @Schema(description = "平均整体评分")
        private BigDecimal avgOverallScore;
    }
}
