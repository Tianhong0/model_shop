package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order detail.
 */
@Data
@Schema(description = "Order detail")
public class OrderDetailVO {

    @Schema(description = "Order ID")
    private Long id;

    @Schema(description = "Order serial number")
    private String orderSn;

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "Model ID")
    private Long modelId;

    @Schema(description = "Model name")
    private String modelName;

    @Schema(description = "Model main image")
    private String mainImageUrl;

    @Schema(description = "Material ID")
    private Long materialId;

    @Schema(description = "Material name")
    private String materialName;

    @Schema(description = "Material color")
    private String materialColor;

    @Schema(description = "Order price")
    private BigDecimal orderPrice;

    @Schema(description = "Order status")
    private Integer orderStatus;

    @Schema(description = "Printer ID")
    private Long printerId;

    @Schema(description = "Custom params JSON")
    private String customParams;

    @Schema(description = "Create time")
    private LocalDateTime createTime;

    @Schema(description = "Update time")
    private LocalDateTime updateTime;

    // ========== 积分和优惠券信息 ==========

    @Schema(description = "基础价格")
    private BigDecimal basePrice;

    @Schema(description = "材料费用")
    private BigDecimal materialCost;

    @Schema(description = "商品总额")
    private BigDecimal goodsAmount;

    @Schema(description = "运费")
    private BigDecimal shippingFee;

    @Schema(description = "使用的积分数量")
    private Integer usedPoints;

    @Schema(description = "积分抵扣金额")
    private BigDecimal pointDiscountAmount;

    @Schema(description = "优惠券ID")
    private Long couponId;

    @Schema(description = "优惠券名称")
    private String couponName;

    @Schema(description = "优惠券抵扣金额")
    private BigDecimal couponDiscountAmount;

    @Schema(description = "获得的积分")
    private Integer earnedPoints;

    // ========== 打印任务状态 ==========

    @Schema(description = "打印任务状态")
    private Integer printJobStatus;

    @Schema(description = "打印任务状态描述")
    private String printJobStatusDesc;

    @Schema(description = "打印进度")
    private BigDecimal printProgress;

    @Schema(description = "打印错误信息")
    private String printErrorMessage;
}
