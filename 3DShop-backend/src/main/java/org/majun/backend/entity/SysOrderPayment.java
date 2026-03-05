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
@TableName("sys_order_payment")
@Schema(description = "Order payment")
public class SysOrderPayment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "Payment ID")
    private Long id;

    @TableField("order_id")
    @Schema(description = "Order ID")
    private Long orderId;

    @TableField("order_sn")
    @Schema(description = "Order serial number")
    private String orderSn;

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
