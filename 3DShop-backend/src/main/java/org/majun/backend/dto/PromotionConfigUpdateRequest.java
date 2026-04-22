package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 推广配置更新请求
 */
@Data
@Schema(description = "推广配置更新请求")
public class PromotionConfigUpdateRequest {

    @NotBlank(message = "配置键不能为空")
    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "配置描述")
    private String configDesc;

    @Schema(description = "状态：1-启用, 0-禁用")
    private Integer status;
}
