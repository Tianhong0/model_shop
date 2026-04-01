package org.majun.backend.vo.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "财务统计")
public class FinanceStatisticsVO {

    @Schema(description = "钱包汇总")
    private WalletSummary walletSummary = new WalletSummary();

    @Schema(description = "流水统计")
    private LedgerStats ledgerStats = new LedgerStats();

    @Schema(description = "提现统计")
    private WithdrawStats withdrawStats = new WithdrawStats();

    @Schema(description = "积分统计")
    private PointStats pointStats = new PointStats();

    @Schema(description = "流水趋势")
    private List<LedgerTrend> ledgerTrend = new ArrayList<>();

    @Data
    @Schema(description = "钱包汇总")
    public static class WalletSummary {
        @Schema(description = "总可用余额")
        private BigDecimal totalAvailableBalance;

        @Schema(description = "总冻结余额")
        private BigDecimal totalFrozenBalance;

        @Schema(description = "账户总数")
        private Long totalAccounts;

        @Schema(description = "正常账户")
        private Long activeAccounts;

        @Schema(description = "冻结账户")
        private Long frozenAccounts;
    }

    @Data
    @Schema(description = "流水统计")
    public static class LedgerStats {
        @Schema(description = "总收入")
        private BigDecimal totalIncome;

        @Schema(description = "总支出")
        private BigDecimal totalExpense;

        @Schema(description = "冻结金额")
        private BigDecimal totalFrozen;

        @Schema(description = "解冻金额")
        private BigDecimal totalUnfrozen;

        @Schema(description = "流水笔数")
        private Long totalTransactions;
    }

    @Data
    @Schema(description = "提现统计")
    public static class WithdrawStats {
        @Schema(description = "申请总数")
        private Long totalApplications;

        @Schema(description = "申请金额")
        private BigDecimal totalAppliedAmount;

        @Schema(description = "已打款金额")
        private BigDecimal totalPaidAmount;

        @Schema(description = "待审核")
        private Long pendingCount;

        @Schema(description = "待打款")
        private Long approvedCount;

        @Schema(description = "已拒绝")
        private Long rejectedCount;

        @Schema(description = "已打款")
        private Long paidCount;

        @Schema(description = "通过率(%)")
        private BigDecimal approvalRate;
    }

    @Data
    @Schema(description = "积分统计")
    public static class PointStats {
        @Schema(description = "总可用积分")
        private Long totalAvailablePoints;

        @Schema(description = "累计获得")
        private Long totalEarned;

        @Schema(description = "累计消耗")
        private Long totalSpent;

        @Schema(description = "账户总数")
        private Long totalAccounts;
    }

    @Data
    @Schema(description = "流水趋势")
    public static class LedgerTrend {
        @Schema(description = "日期")
        private String date;

        @Schema(description = "收入")
        private BigDecimal income;

        @Schema(description = "支出")
        private BigDecimal expense;
    }
}
