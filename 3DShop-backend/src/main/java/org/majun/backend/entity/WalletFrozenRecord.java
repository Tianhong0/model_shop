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

/**
 * 钱包冻结记录 - 用于二手交易资金冻结
 */
@Data
@TableName("wallet_frozen_record")
public class WalletFrozenRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("account_id")
    private Long accountId;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("biz_type")
    private String bizType;

    @TableField("biz_no")
    private String bizNo;

    @TableField("ref_id")
    private Long refId;

    /**
     * 冻结天数
     */
    @TableField("frozen_days")
    private Integer frozenDays;

    /**
     * 冻结开始时间
     */
    @TableField("frozen_start_time")
    private LocalDateTime frozenStartTime;

    /**
     * 冻结到期时间
     */
    @TableField("frozen_end_time")
    private LocalDateTime frozenEndTime;

    /**
     * 状态: 0-冻结中, 1-已解冻, 2-已取消
     */
    @TableField("status")
    private Integer status;

    /**
     * 解冻时间
     */
    @TableField("unfreeze_time")
    private LocalDateTime unfreezeTime;

    @TableField("remark")
    private String remark;

    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
