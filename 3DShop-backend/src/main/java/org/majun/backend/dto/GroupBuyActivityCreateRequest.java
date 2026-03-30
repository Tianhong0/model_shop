package org.majun.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拼团活动创建请求
 */
@Data
@Schema(description = "拼团活动创建请求")
public class GroupBuyActivityCreateRequest {

    @NotBlank(message = "活动名称不能为空")
    @Schema(description = "活动名称")
    private String activityName;

    @NotNull(message = "模型ID不能为空")
    @Schema(description = "关联模型ID")
    private Long modelId;

    @Min(value = 2, message = "最小拼团人数不能小于2")
    @Schema(description = "最小拼团人数")
    private Integer minPeople = 2;

    @Schema(description = "最大拼团人数")
    private Integer maxPeople;

    @NotNull(message = "折扣类型不能为空")
    @Schema(description = "折扣类型：1-固定折扣，2-阶梯折扣")
    private Integer discountType;

    @DecimalMin(value = "1", message = "折扣值必须大于0")
    @Schema(description = "折扣值（百分比，如90表示9折）")
    private BigDecimal discountValue;

    @Schema(description = "阶梯折扣配置JSON")
    private String ladderConfig;

    @NotNull(message = "原价不能为空")
    @DecimalMin(value = "0.01", message = "原价必须大于0")
    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @NotNull(message = "拼团价不能为空")
    @DecimalMin(value = "0.01", message = "拼团价必须大于0")
    @Schema(description = "拼团价")
    private BigDecimal groupPrice;

    @NotNull(message = "开始时间不能为空")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "活动开始时间")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "活动结束时间")
    private LocalDateTime endTime;

    @Min(value = 1, message = "超时时间不能小于1小时")
    @Schema(description = "拼团超时时间（小时）")
    private Integer timeoutHours = 24;

    @Schema(description = "总库存")
    private Integer totalStock;

    @Schema(description = "活动封面图")
    private String coverImage;

    @Schema(description = "活动描述")
    private String description;
}
