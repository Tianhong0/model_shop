package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_order_delivery_track")
@Schema(description = "Order delivery track")
public class SysOrderDeliveryTrack implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "Track ID")
    private Long id;

    @TableField("delivery_id")
    @Schema(description = "Delivery ID")
    private Long deliveryId;

    @TableField("track_content")
    @Schema(description = "Track content")
    private String trackContent;

    @TableField("track_time")
    @Schema(description = "Track time")
    private LocalDateTime trackTime;

    @TableField("operator_info")
    @Schema(description = "Operator info")
    private String operatorInfo;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "Create time")
    private LocalDateTime createTime;
}
