package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wallet_ledger")
public class WalletLedger implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("account_id")
    private Long accountId;

    @TableField("direction")
    private Integer direction;

    @TableField("biz_type")
    private String bizType;

    @TableField("biz_no")
    private String bizNo;

    @TableField("ref_id")
    private Long refId;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("before_available")
    private BigDecimal beforeAvailable;

    @TableField("after_available")
    private BigDecimal afterAvailable;

    @TableField("before_frozen")
    private BigDecimal beforeFrozen;

    @TableField("after_frozen")
    private BigDecimal afterFrozen;

    @TableField("remark")
    private String remark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
