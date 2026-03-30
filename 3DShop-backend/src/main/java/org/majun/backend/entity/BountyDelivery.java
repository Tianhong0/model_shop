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
import java.time.LocalDateTime;

@Data
@TableName("bounty_delivery")
public class BountyDelivery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("task_id")
    private Long taskId;

    @TableField("bid_id")
    private Long bidId;

    @TableField("designer_id")
    private Long designerId;

    @TableField("delivery_round")
    private Integer deliveryRound;

    @TableField("description")
    private String description;

    @TableField("status")
    private Integer status;

    @TableField("is_final")
    private Integer isFinal;

    @TableField("allow_commercial_use")
    private Integer allowCommercialUse;

    @TableField("allow_modification")
    private Integer allowModification;

    @TableField("license_type")
    private String licenseType;

    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
