package org.majun.backend.vo.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "用户统计")
public class UserStatisticsVO {

    @Schema(description = "汇总数据")
    private UserSummary summary = new UserSummary();

    @Schema(description = "增长趋势")
    private List<GrowthTrend> growthTrend = new ArrayList<>();

    @Schema(description = "角色分布")
    private List<RoleDistribution> roleDistribution = new ArrayList<>();

    @Schema(description = "设计者统计")
    private DesignerStats designerStats = new DesignerStats();

    @Data
    @Schema(description = "用户汇总")
    public static class UserSummary {
        @Schema(description = "用户总数")
        private Long totalUsers;

        @Schema(description = "新增用户(时间段内)")
        private Long newUsers;

        @Schema(description = "活跃用户")
        private Long activeUsers;

        @Schema(description = "正常用户")
        private Long normalUsers;

        @Schema(description = "禁用用户")
        private Long disabledUsers;

        @Schema(description = "男性用户")
        private Long maleUsers;

        @Schema(description = "女性用户")
        private Long femaleUsers;
    }

    @Data
    @Schema(description = "增长趋势")
    public static class GrowthTrend {
        @Schema(description = "日期")
        private String date;

        @Schema(description = "新增用户数")
        private Integer newUserCount;

        @Schema(description = "累计用户数")
        private Long totalUserCount;
    }

    @Data
    @Schema(description = "角色分布")
    public static class RoleDistribution {
        @Schema(description = "角色ID")
        private Long roleId;

        @Schema(description = "角色名称")
        private String roleName;

        @Schema(description = "用户数")
        private Long userCount;

        @Schema(description = "占比(%)")
        private BigDecimal percentage;
    }

    @Data
    @Schema(description = "设计者统计")
    public static class DesignerStats {
        @Schema(description = "设计者总数")
        private Long totalDesigners;

        @Schema(description = "新增设计者")
        private Long newDesigners;

        @Schema(description = "待审核申请")
        private Long pendingApplications;

        @Schema(description = "已通过申请")
        private Long approvedApplications;

        @Schema(description = "已拒绝申请")
        private Long rejectedApplications;

        @Schema(description = "平均信誉分")
        private BigDecimal avgCreditScore;

        @Schema(description = "平均评分")
        private BigDecimal avgRating;
    }
}
