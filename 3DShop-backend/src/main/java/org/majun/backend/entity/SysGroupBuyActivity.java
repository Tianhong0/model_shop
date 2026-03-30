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

/**
 * 拼团活动实体
 */
@Data
@TableName("sys_group_buy_activity")
@Schema(description = "拼团活动")
public class SysGroupBuyActivity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "活动ID")
    private Long id;

    @TableField("activity_name")
    @Schema(description = "活动名称")
    private String activityName;

    @TableField("model_id")
    @Schema(description = "关联模型ID")
    private Long modelId;

    @TableField("min_people")
    @Schema(description = "最小拼团人数")
    private Integer minPeople;

    @TableField("max_people")
    @Schema(description = "最大拼团人数")
    private Integer maxPeople;

    @TableField("discount_type")
    @Schema(description = "折扣类型：1-固定折扣，2-阶梯折扣")
    private Integer discountType;

    @TableField("discount_value")
    @Schema(description = "折扣值")
    private BigDecimal discountValue;

    @TableField("ladder_config")
    @Schema(description = "阶梯折扣配置JSON")
    private String ladderConfig;

    @TableField("original_price")
    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @TableField("group_price")
    @Schema(description = "拼团价")
    private BigDecimal groupPrice;

    @TableField("start_time")
    @Schema(description = "活动开始时间")
    private LocalDateTime startTime;

    @TableField("end_time")
    @Schema(description = "活动结束时间")
    private LocalDateTime endTime;

    @TableField("timeout_hours")
    @Schema(description = "拼团超时时间（小时）")
    private Integer timeoutHours;

    @TableField("status")
    @Schema(description = "状态：0-禁用，1-启用，2-已结束")
    private Integer status;

    @TableField("total_stock")
    @Schema(description = "总库存")
    private Integer totalStock;

    @TableField("sold_count")
    @Schema(description = "已售数量")
    private Integer soldCount;

    @TableField("cover_image")
    @Schema(description = "活动封面图")
    private String coverImage;

    @TableField("description")
    @Schema(description = "活动描述")
    private String description;

    @TableField("is_delete")
    @TableLogic
    @Schema(description = "逻辑删除")
    private Integer isDelete;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
