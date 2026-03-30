package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 批量打印折扣配置实体
 */
@Data
@TableName("sys_batch_print_discount")
@Schema(description = "批量打印折扣配置")
public class SysBatchPrintDiscount implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "配置ID")
    private Long id;

    @TableField("min_quantity")
    @Schema(description = "最小数量")
    private Integer minQuantity;

    @TableField("max_quantity")
    @Schema(description = "最大数量")
    private Integer maxQuantity;

    @TableField("discount_percent")
    @Schema(description = "折扣百分比")
    private BigDecimal discountPercent;

    @TableField("description")
    @Schema(description = "描述")
    private String description;

    @TableField("is_active")
    @Schema(description = "是否启用")
    private Integer isActive;

    @TableField("sort_order")
    @Schema(description = "排序")
    private Integer sortOrder;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
