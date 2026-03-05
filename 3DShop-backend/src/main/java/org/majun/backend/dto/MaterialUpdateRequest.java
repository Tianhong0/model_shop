package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;

/**
 * 更新材质请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "更新材质请求")
public class MaterialUpdateRequest {

    /**
     * 材质ID
     */
    @Schema(description = "材质ID")
    @NotNull(message = "材质ID不能为空")
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
}