package org.majun.backend.vo.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "模型统计")
public class ModelStatisticsVO {

    @Schema(description = "汇总数据")
    private ModelSummary summary = new ModelSummary();

    @Schema(description = "增长趋势")
    private List<ModelTrend> trend = new ArrayList<>();

    @Schema(description = "分类分布")
    private List<CategoryDistribution> categoryDistribution = new ArrayList<>();

    @Schema(description = "状态分布")
    private List<StatusStats> statusDistribution = new ArrayList<>();

    @Schema(description = "热门模型TOP10")
    private List<TopModel> topModels = new ArrayList<>();

    @Data
    @Schema(description = "模型汇总")
    public static class ModelSummary {
        @Schema(description = "模型总数")
        private Long totalModels;

        @Schema(description = "新增模型(时间段内)")
        private Long newModels;

        @Schema(description = "上架模型")
        private Long activeModels;

        @Schema(description = "下架模型")
        private Long inactiveModels;

        @Schema(description = "审核中")
        private Long pendingModels;

        @Schema(description = "总收藏数")
        private Long totalFavorites;

        @Schema(description = "平均价格")
        private BigDecimal avgPrice;
    }

    @Data
    @Schema(description = "模型趋势")
    public static class ModelTrend {
        @Schema(description = "日期")
        private String date;

        @Schema(description = "新增模型数")
        private Integer newModelCount;

        @Schema(description = "新增收藏数")
        private Integer newFavoriteCount;
    }

    @Data
    @Schema(description = "分类分布")
    public static class CategoryDistribution {
        @Schema(description = "分类ID")
        private Long categoryId;

        @Schema(description = "分类名称")
        private String categoryName;

        @Schema(description = "模型数")
        private Long modelCount;

        @Schema(description = "占比(%)")
        private BigDecimal percentage;
    }

    @Data
    @Schema(description = "状态统计")
    public static class StatusStats {
        @Schema(description = "状态码")
        private Integer status;

        @Schema(description = "状态名称")
        private String statusName;

        @Schema(description = "数量")
        private Long count;
    }

    @Data
    @Schema(description = "热门模型")
    public static class TopModel {
        @Schema(description = "模型ID")
        private Long modelId;

        @Schema(description = "模型名称")
        private String modelName;

        @Schema(description = "设计者")
        private String designerName;

        @Schema(description = "收藏数")
        private Long favoriteCount;

        @Schema(description = "价格")
        private BigDecimal price;
    }
}
