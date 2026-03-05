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
 * 3D模型信息实体类
 */
@Data
@TableName("sys_model")
@Schema(description = "3D模型信息")
public class SysModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID (雪花算法)
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 模型名称
     */
    @TableField("model_name")
    @Schema(description = "模型名称")
    private String modelName;


    /**
     * 模型描述
     */
    @TableField("description")
    @Schema(description = "模型描述")
    private String description;

    /**
     * 设计者ID (关联sys_user.id)
     */
    @TableField("designer_id")
    @Schema(description = "设计者ID")
    private Long designerId;

    /**
     * 分类ID (关联sys_model_category.id)
     */
    @TableField("category_id")
    @Schema(description = "分类ID")
    private Long categoryId;

    /**
     * 基础价格
     */
    @TableField("base_price")
    @Schema(description = "基础价格")
    private BigDecimal basePrice;

    /**
     * 原始体积 (mm³)
     */
    @TableField("base_volume")
    @Schema(description = "原始体积(mm³)")
    private BigDecimal baseVolume;

    /**
     * 原始三维尺寸
     */
    @TableField("base_size")
    @Schema(description = "原始三维尺寸")
    private String baseSize;

    /**
     * 模型文件路径 (云存储/MinIO地址)
     */
    @TableField("file_path")
    @Schema(description = "模型文件路径")
    private String filePath;

    /**
     * 授权说明
     */
    @TableField("license_type")
    @Schema(description = "授权说明")
    private String licenseType;

    /**
     * 上架状态: 0-审核中, 1-上架, 2-下架
     */
    @TableField("status")
    @Schema(description = "上架状态: 0-审核中, 1-上架, 2-下架")
    private Integer status;

    /**
     * 逻辑删除: 1-已删, 0-未删
     */
    @TableField("is_delete")
    @Schema(description = "逻辑删除")
    private Integer isDelete;

    /**
     * 创建时间
     */
    @TableField("create_time")
    @Schema(description = "创建时间")
    private String createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    @Schema(description = "更新时间")
    private String updateTime;



}
