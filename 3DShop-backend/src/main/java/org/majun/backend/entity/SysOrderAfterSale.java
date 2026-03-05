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
@TableName("sys_order_after_sale")
@Schema(description = "Order after-sale")
public class SysOrderAfterSale implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("after_sale_sn")
    private String afterSaleSn;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_sn")
    private String orderSn;

    @TableField("user_id")
    private Long userId;

    @TableField("type")
    private Integer type;

    @TableField("reason")
    private String reason;

    @TableField("description")
    private String description;

    @TableField("evidence_urls")
    private String evidenceUrls;

    @TableField("requested_amount")
    private BigDecimal requestedAmount;

    @TableField("approved_amount")
    private BigDecimal approvedAmount;

    @TableField("status")
    private Integer status;

    @TableField("refund_status")
    private Integer refundStatus;

    @TableField("admin_remark")
    private String adminRemark;

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
