package org.majun.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拼团活动更新请求
 */
@Data
@Schema(description = "拼团活动更新请求")
public class GroupBuyActivityUpdateRequest {

    @NotNull(message = "活动ID不能为空")
    @Schema(description = "活动ID")
    private Long id;

    @Schema(description = "活动名称")
    private String activityName;

    @Schema(description = "最小拼团人数")
    private Integer minPeople;

    @Schema(description = "最大拼团人数")
    private Integer maxPeople;

    @Schema(description = "折扣类型")
    private Integer discountType;

    @Schema(description = "折扣值")
    private BigDecimal discountValue;

    @Schema(description = "阶梯折扣配置JSON")
    private String ladderConfig;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "拼团价")
    private BigDecimal groupPrice;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "活动开始时间")
    private LocalDateTime startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "活动结束时间")
    private LocalDateTime endTime;

    @Schema(description = "拼团超时时间（小时）")
    private Integer timeoutHours;

    @Schema(description = "总库存")
    private Integer totalStock;

    @Schema(description = "活动封面图")
    private String coverImage;

    @Schema(description = "活动描述")
    private String description;
}
