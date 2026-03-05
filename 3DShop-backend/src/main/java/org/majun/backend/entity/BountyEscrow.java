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
@TableName("bounty_escrow")
public class BountyEscrow implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("task_id")
    private Long taskId;

    @TableField("payer_id")
    private Long payerId;

    @TableField("pay_batch_id")
    private Long payBatchId;

    @TableField("out_trade_no")
    private String outTradeNo;

    @TableField("escrow_amount")
    private BigDecimal escrowAmount;

    @TableField("released_amount")
    private BigDecimal releasedAmount;

    @TableField("refund_amount")
    private BigDecimal refundAmount;

    @TableField("status")
    private Integer status;

    @TableField("version")
    private Integer version;

    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
