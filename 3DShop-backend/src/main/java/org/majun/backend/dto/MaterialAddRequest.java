package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;

/**
 * 添加材质请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "添加材质请求")
public class MaterialAddRequest {

    /**
     * 材质ID（如果是新材质则为null）
     */
    @Schema(description = "材质ID（如果是新材质则为null）")
    private Long materialId;

    /**
     * 材质名称
     */
    @Schema(description = "材质名称")
    @NotBlank(message = "材质名称不能为空")
    private String name;

    /**
     * 单价
     */
    @Schema(description = "单价")
    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0.01", message = "单价必须大于0")
    private Double price;

    /**
     * 是否信任材质
     */
    @Schema(description = "是否信任材质")
    private Boolean isTrusted;

    /**
     * 是否环保材质
     */
    @Schema(description = "是否环保材质")
    private Boolean isEco;
}