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
import java.time.LocalDateTime;

@Data
@TableName("sys_order_delivery")
@Schema(description = "Order delivery")
public class SysOrderDelivery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "Delivery ID")
    private Long id;

    @TableField("order_id")
    @Schema(description = "Order ID")
    private Long orderId;

    @TableField("order_sn")
    @Schema(description = "Order serial number")
    private String orderSn;

    @TableField("delivery_company")
    @Schema(description = "Delivery company")
    private String deliveryCompany;

    @TableField("delivery_sn")
    @Schema(description = "Delivery serial number")
    private String deliverySn;

    @TableField("receiver_name")
    @Schema(description = "Receiver name")
    private String receiverName;

    @TableField("receiver_phone")
    @Schema(description = "Receiver phone")
    private String receiverPhone;

    @TableField("receiver_address")
    @Schema(description = "Receiver address")
    private String receiverAddress;

    @TableField("status")
    @Schema(description = "Delivery status")
    private Integer status;

    @TableField("delivery_time")
    @Schema(description = "Delivery time")
    private LocalDateTime deliveryTime;

    @TableField("receive_time")
    @Schema(description = "Receive time")
    private LocalDateTime receiveTime;

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
