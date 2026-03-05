package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Order comment stats")
public class OrderCommentStatsVO {

    @Schema(description = "Model ID")
    private Long modelId;

    @Schema(description = "Comment count")
    private Long totalCount;

    @Schema(description = "Average model score")
    private Double avgModelScore;

    @Schema(description = "Average print score")
    private Double avgPrintScore;

    @Schema(description = "Average service score")
    private Double avgServiceScore;

    @Schema(description = "Average overall score")
    private Double avgOverallScore;
}
