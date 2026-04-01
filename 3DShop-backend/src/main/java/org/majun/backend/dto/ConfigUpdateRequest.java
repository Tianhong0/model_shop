package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 系统配置更新请求
 */
@Data
@Schema(description = "系统配置更新请求")
public class ConfigUpdateRequest {

    @NotBlank(message = "配置键不能为空")
    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "配置类型")
    private String configType;

    @Schema(description = "配置分组")
    private String configGroup;

    @Schema(description = "配置说明")
    private String description;

    @Schema(description = "是否公开")
    private Integer isPublic;
}
