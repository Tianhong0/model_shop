package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统配置VO
 */
@Data
@Schema(description = "系统配置")
public class ConfigVO {

    @Schema(description = "配置ID")
    private Long id;

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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
