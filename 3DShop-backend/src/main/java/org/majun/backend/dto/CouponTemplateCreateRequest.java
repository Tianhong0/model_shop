package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建优惠券模板请求
 */
@Data
@Schema(description = "创建优惠券模板请求")
public class CouponTemplateCreateRequest {

    @NotBlank(message = "优惠券名称不能为空")
    @Size(max = 100, message = "优惠券名称最长100字符")
    @Schema(description = "优惠券名称")
    private String name;

    @NotNull(message = "类型不能为空")
    @Min(value = 1, message = "类型值无效")
    @Max(value = 3, message = "类型值无效")
    @Schema(description = "类型：1-满减券，2-折扣券，3-现金券")
    private Integer type;

    @NotNull(message = "优惠值不能为空")
    @DecimalMin(value = "0.01", message = "优惠值必须大于0")
    @Schema(description = "优惠值（满减金额/折扣比例如0.8/现金金额）")
    private BigDecimal value;

    @DecimalMin(value = "0", message = "最低消费门槛不能为负")
    @Schema(description = "最低消费门槛")
    private BigDecimal minAmount = BigDecimal.ZERO;

    @Schema(description = "最大优惠金额（折扣券用）")
    private BigDecimal maxDiscount;

    @NotNull(message = "兑换所需积分不能为空")
    @Min(value = 1, message = "兑换所需积分必须大于0")
    @Schema(description = "兑换所需积分")
    private Integer pointCost;

    @NotNull(message = "总库存不能为空")
    @Min(value = 1, message = "总库存必须大于0")
    @Schema(description = "总库存")
    private Integer totalStock;

    @Min(value = 1, message = "每人限领数量必须大于0")
    @Schema(description = "每人限领数量")
    private Integer perUserLimit = 1;

    @Min(value = 1, message = "有效天数必须大于0")
    @Schema(description = "有效天数（从领取日开始）")
    private Integer validDays = 30;

    @Schema(description = "有效期开始时间（固定有效期用）")
    private LocalDateTime startTime;

    @Schema(description = "有效期结束时间（固定有效期用）")
    private LocalDateTime endTime;

    @Schema(description = "使用说明")
    private String description;
}
