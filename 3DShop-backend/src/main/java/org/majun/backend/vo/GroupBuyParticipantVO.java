package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拼团参与VO
 */
@Data
@Schema(description = "拼团参与VO")
public class GroupBuyParticipantVO {

    @Schema(description = "参与ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户昵称")
    private String userNickname;

    @Schema(description = "用户头像")
    private String userAvatar;

    @Schema(description = "是否团长")
    private Boolean isLeader;

    @Schema(description = "购买数量")
    private Integer quantity;

    @Schema(description = "单价")
    private BigDecimal unitPrice;

    @Schema(description = "总金额")
    private BigDecimal totalAmount;

    @Schema(description = "材质ID")
    private Long materialId;

    @Schema(description = "材质名称")
    private String materialName;

    @Schema(description = "颜色")
    private String color;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单号")
    private String orderSn;
}
