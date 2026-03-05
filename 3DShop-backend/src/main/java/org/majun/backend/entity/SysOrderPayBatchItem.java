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

@Data
@TableName("sys_order_pay_batch_item")
@Schema(description = "Order batch payment item")
public class SysOrderPayBatchItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "Item ID")
    private Long id;

    @TableField("batch_id")
    @Schema(description = "Batch ID")
    private Long batchId;

    @TableField("order_id")
    @Schema(description = "Order ID")
    private Long orderId;

    @TableField("order_sn")
    @Schema(description = "Order serial number")
    private String orderSn;

    @TableField("order_amount")
    @Schema(description = "Order amount")
    private BigDecimal orderAmount;

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
