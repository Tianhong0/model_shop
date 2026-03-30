package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 模型材质关联实体类
 */
@Data
@TableName("sys_model_material")
@Schema(description = "模型材质关联")
public class ModelMaterial implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 模型ID
     */
    @TableField("model_id")
    @Schema(description = "模型ID")
    private Long modelId;

    /**
     * 材质ID
     */
    @TableField("material_id")
    @Schema(description = "材质ID")
    private Long materialId;

    /**
     * 材质名称
     */
    @TableField("material_name")
    @Schema(description = "材质名称")
    private String materialName;

    /**
     * 单价 (元/克)
     */
    @TableField("price")
    @Schema(description = "单价(元/克)")
    private Double price;

    /**
     * 是否信任材质
     */
    @TableField("is_trusted")
    @Schema(description = "是否信任材质")
    private Boolean isTrusted;

    /**
     * 是否环保材质
     */
    @TableField("is_eco")
    @Schema(description = "是否环保材质")
    private Boolean isEco;

    /**
     * 创建时间
     */
    @TableField("create_time")
    @Schema(description = "创建时间")
    private String createTime;
}