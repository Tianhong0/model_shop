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
@TableName("bounty_task")
public class BountyTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("task_sn")
    private String taskSn;

    @TableField("publisher_id")
    private Long publisherId;

    @TableField("title")
    private String title;

    @TableField("description")
    private String description;

    @TableField("category")
    private String category;

    @TableField("tags")
    private String tags;

    @TableField("budget_amount")
    private BigDecimal budgetAmount;

    @TableField("final_amount")
    private BigDecimal finalAmount;

    @TableField("expected_days")
    private Integer expectedDays;

    @TableField("deadline_time")
    private LocalDateTime deadlineTime;

    @TableField("status")
    private Integer status;

    @TableField("winner_bid_id")
    private Long winnerBidId;

    @TableField("winner_designer_id")
    private Long winnerDesignerId;

    @TableField("version")
    private Integer version;

    @TableField("close_reason")
    private String closeReason;

    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
