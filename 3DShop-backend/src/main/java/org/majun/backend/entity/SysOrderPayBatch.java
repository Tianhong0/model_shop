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
@TableName("sys_order_pay_batch")
@Schema(description = "Order batch payment")
public class SysOrderPayBatch implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "Batch ID")
    private Long id;

    @TableField("user_id")
    @Schema(description = "User ID")
    private Long userId;

    @TableField("out_trade_no")
    @Schema(description = "Merchant trade number")
    private String outTradeNo;

    @TableField("trade_no")
    @Schema(description = "Alipay trade number")
    private String tradeNo;

    @TableField("total_amount")
    @Schema(description = "Payment amount")
    private BigDecimal totalAmount;

    @TableField("pay_channel")
    @Schema(description = "Payment channel")
    private String payChannel;

    @TableField("pay_status")
    @Schema(description = "Payment status")
    private Integer payStatus;

    @TableField("notify_content")
    @Schema(description = "Raw notify content")
    private String notifyContent;

    @TableField("pay_time")
    @Schema(description = "Pay time")
    private LocalDateTime payTime;

    @TableField("close_time")
    @Schema(description = "Close time")
    private LocalDateTime closeTime;

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
