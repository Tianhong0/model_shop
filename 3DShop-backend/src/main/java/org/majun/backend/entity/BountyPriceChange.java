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
@TableName("bounty_price_change")
public class BountyPriceChange implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("task_id")
    private Long taskId;

    @TableField("apply_by")
    private Long applyBy;

    @TableField("current_amount")
    private BigDecimal currentAmount;

    @TableField("target_amount")
    private BigDecimal targetAmount;

    @TableField("reason")
    private String reason;

    @TableField("status")
    private Integer status;

    @TableField("confirm_by")
    private Long confirmBy;

    @TableField("confirm_time")
    private LocalDateTime confirmTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
