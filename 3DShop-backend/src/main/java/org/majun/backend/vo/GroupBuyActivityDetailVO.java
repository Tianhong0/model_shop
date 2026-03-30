package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拼团活动详情VO
 */
@Data
@Schema(description = "拼团活动详情VO")
public class GroupBuyActivityDetailVO {

    @Schema(description = "活动ID")
    private Long id;

    @Schema(description = "活动名称")
    private String activityName;

    @Schema(description = "模型ID")
    private Long modelId;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "模型主图")
    private String modelImage;

    @Schema(description = "模型描述")
    private String modelDescription;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "拼团价")
    private BigDecimal groupPrice;

    @Schema(description = "最小拼团人数")
    private Integer minPeople;

    @Schema(description = "最大拼团人数")
    private Integer maxPeople;

    @Schema(description = "折扣类型")
    private Integer discountType;

    @Schema(description = "折扣值")
    private BigDecimal discountValue;

    @Schema(description = "阶梯折扣配置")
    private String ladderConfig;

    @Schema(description = "活动开始时间")
    private LocalDateTime startTime;

    @Schema(description = "活动结束时间")
    private LocalDateTime endTime;

    @Schema(description = "超时时间（小时）")
    private Integer timeoutHours;

    @Schema(description = "已售数量")
    private Integer soldCount;

    @Schema(description = "总库存")
    private Integer totalStock;

    @Schema(description = "活动封面图")
    private String coverImage;

    @Schema(description = "活动描述")
    private String description;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "模型材质列表")
    private java.util.List<MaterialVO> materials;
}
