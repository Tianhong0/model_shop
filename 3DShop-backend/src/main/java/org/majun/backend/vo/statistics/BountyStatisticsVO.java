package org.majun.backend.vo.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "悬赏统计")
public class BountyStatisticsVO {

    @Schema(description = "任务汇总")
    private TaskSummary taskSummary = new TaskSummary();

    @Schema(description = "任务趋势")
    private List<TaskTrend> taskTrend = new ArrayList<>();

    @Schema(description = "状态分布")
    private List<StatusDistribution> statusDistribution = new ArrayList<>();

    @Schema(description = "评分统计")
    private RatingStats ratingStats = new RatingStats();

    @Schema(description = "设计师排行TOP10")
    private List<TopDesigner> topDesigners = new ArrayList<>();

    @Data
    @Schema(description = "任务汇总")
    public static class TaskSummary {
        @Schema(description = "任务总数")
        private Long totalTasks;

        @Schema(description = "新增任务(时间段内)")
        private Long newTasks;

        @Schema(description = "已完成任务")
        private Long completedTasks;

        @Schema(description = "进行中任务")
        private Long ongoingTasks;

        @Schema(description = "已关闭任务")
        private Long closedTasks;

        @Schema(description = "总预算金额")
        private BigDecimal totalBudgetAmount;

        @Schema(description = "总成交金额")
        private BigDecimal totalFinalAmount;

        @Schema(description = "平均成交金额")
        private BigDecimal avgFinalAmount;
    }

    @Data
    @Schema(description = "任务趋势")
    public static class TaskTrend {
        @Schema(description = "日期")
        private String date;

        @Schema(description = "新增任务数")
        private Integer newTaskCount;

        @Schema(description = "完成任务数")
        private Integer completedTaskCount;

        @Schema(description = "成交金额")
        private BigDecimal finalAmount;
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
    @Schema(description = "评分统计")
    public static class RatingStats {
        @Schema(description = "评价总数")
        private Long totalRatings;

        @Schema(description = "平均评分")
        private BigDecimal avgRating;

        @Schema(description = "五星评价数")
        private Long fiveStarCount;

        @Schema(description = "四星评价数")
        private Long fourStarCount;

        @Schema(description = "三星评价数")
        private Long threeStarCount;

        @Schema(description = "二星评价数")
        private Long twoStarCount;

        @Schema(description = "一星评价数")
        private Long oneStarCount;

        @Schema(description = "申诉总数")
        private Long totalAppeals;

        @Schema(description = "申诉成功数")
        private Long successAppeals;
    }

    @Data
    @Schema(description = "热门设计师")
    public static class TopDesigner {
        @Schema(description = "用户ID")
        private Long userId;

        @Schema(description = "昵称")
        private String nickname;

        @Schema(description = "完成任务数")
        private Long completedTasks;

        @Schema(description = "总收入")
        private BigDecimal totalIncome;

        @Schema(description = "平均评分")
        private BigDecimal avgRating;

        @Schema(description = "信誉分")
        private Integer creditScore;
    }
}
