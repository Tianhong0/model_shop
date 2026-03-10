package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("used_order_payment")
public class UsedOrderPayment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_sn")
    private String orderSn;

    @TableField("out_trade_no")
    private String outTradeNo;

    @TableField("trade_no")
    private String tradeNo;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("pay_channel")
    private String payChannel;

    @TableField("pay_status")
    private Integer payStatus;

    @TableField("notify_content")
    private String notifyContent;

    @TableField("pay_time")
    private LocalDateTime payTime;

    @TableField("close_time")
    private LocalDateTime closeTime;

    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
