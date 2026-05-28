package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 设计师分润结算实体类
 */
@Data
@TableName("designer_settlement")
@Schema(description = "设计师分润结算")
public class DesignerSettlement implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("settlement_sn")
    @Schema(description = "结算流水号")
    private String settlementSn;

    @TableField("order_id")
    @Schema(description = "关联订单ID")
    private Long orderId;

    @TableField("order_sn")
    @Schema(description = "关联订单编号")
    private String orderSn;

    @TableField("model_id")
    @Schema(description = "关联模型ID")
    private Long modelId;

    @TableField("designer_id")
    @Schema(description = "设计师用户ID")
    private Long designerId;

    @TableField("order_price")
    @Schema(description = "订单金额")
    private BigDecimal orderPrice;

    @TableField("profit_share_ratio")
    @Schema(description = "当时的分润比例")
    private Integer profitShareRatio;

    @TableField("settlement_amount")
    @Schema(description = "结算金额(分润后)")
    private BigDecimal settlementAmount;

    @TableField("biz_type")
    @Schema(description = "业务类型")
    private String bizType;

    @TableField("status")
    @Schema(description = "结算状态: 0-待结算, 1-已结算, 2-结算失败")
    private Integer status;

    @TableField("wallet_ledger_id")
    @Schema(description = "关联钱包流水ID")
    private Long walletLedgerId;

    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    @TableField("is_delete")
    @Schema(description = "逻辑删除")
    private Integer isDelete;

    @TableField("create_time")
    @Schema(description = "创建时间")
    private String createTime;

    @TableField("update_time")
    @Schema(description = "更新时间")
    private String updateTime;
}
