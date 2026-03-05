package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order entity.
 */
@Data
@TableName("sys_order")
@Schema(description = "Order")
public class SysOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "Order ID")
    private Long id;

    @TableField("order_sn")
    @Schema(description = "Order serial number")
    private String orderSn;

    @TableField("user_id")
    @Schema(description = "User ID")
    private Long userId;

    @TableField("model_id")
    @Schema(description = "Model ID")
    private Long modelId;

    @TableField("material_id")
    @Schema(description = "Material ID")
    private Long materialId;

    @TableField("order_price")
    @Schema(description = "Order price")
    private BigDecimal orderPrice;

    @TableField("order_status")
    @Schema(description = "Order status")
    private Integer orderStatus;

    @TableField("custom_params")
    @Schema(description = "Custom parameters JSON")
    private String customParams;

    @TableField("printer_id")
    @Schema(description = "Printer ID")
    private Long printerId;

    @TableField("is_delete")
    @TableLogic
    @Schema(description = "Logical delete flag")
    private Integer isDelete;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "Create time")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "Update time")
    private LocalDateTime updateTime;
}
