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
 * 材质价格实体类
 */
@Data
@TableName("sys_material")
@Schema(description = "材质价格")
public class SysMaterial implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 材质名称 (如: 光敏树脂)
     */
    @TableField("name")
    @Schema(description = "材质名称")
    private String name;

    /**
     * 颜色 (如: 纯白)
     */
    @TableField("color")
    @Schema(description = "颜色")
    private String color;

    /**
     * 单价 (元/克)
     */
    @TableField("price")
    @Schema(description = "单价(元/克)")
    private BigDecimal price;

    /**
     * 状态: 1-可用, 0-停用
     */
    @TableField("status")
    @Schema(description = "状态: 1-可用, 0-停用")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField("create_time")
    @Schema(description = "创建时间")
    private String createTime;
}
