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
@TableName("used_after_sale")
public class UsedAfterSale implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("after_sale_sn")
    private String afterSaleSn;

    @TableField("order_id")
    private Long orderId;

    @TableField("buyer_id")
    private Long buyerId;

    @TableField("seller_id")
    private Long sellerId;

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

    @TableField("refund_amount")
    private BigDecimal refundAmount;

    @TableField("status")
    private Integer status;

    @TableField("seller_remark")
    private String sellerRemark;

    @TableField("admin_remark")
    private String adminRemark;

    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
