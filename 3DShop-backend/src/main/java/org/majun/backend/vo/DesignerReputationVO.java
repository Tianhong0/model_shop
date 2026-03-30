package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 设计者信誉展示VO
 */
@Data
@Schema(description = "设计者信誉信息")
public class DesignerReputationVO {

    @Schema(description = "设计者ID")
    private Long designerId;

    @Schema(description = "设计者名称")
    private String designerName;

    @Schema(description = "设计者头像")
    private String avatar;

    @Schema(description = "信誉评分(默认80分)")
    private Integer reputationScore;

    @Schema(description = "完成任务总数")
    private Integer totalTasks;

    @Schema(description = "被评价总数")
    private Integer totalRatings;

    @Schema(description = "平均评分")
    private BigDecimal avgScore;

    @Schema(description = "五星好评数")
    private Integer fiveStarCount;

    @Schema(description = "四星好评数")
    private Integer fourStarCount;

    @Schema(description = "三星评价数")
    private Integer threeStarCount;

    @Schema(description = "二星评价数")
    private Integer twoStarCount;

    @Schema(description = "一星差评数")
    private Integer oneStarCount;

    @Schema(description = "优质回答数")
    private Integer qualityAnswerCount;
}
