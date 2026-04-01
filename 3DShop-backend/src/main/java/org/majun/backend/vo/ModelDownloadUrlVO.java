package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型下载URL响应VO
 */
@Data
@Schema(description = "模型下载URL响应")
public class ModelDownloadUrlVO {

    @Schema(description = "下载URL")
    private String downloadUrl;

    @Schema(description = "URL过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "过期秒数")
    private Integer expireSeconds;

    @Schema(description = "模型ID")
    private Long modelId;

    @Schema(description = "模型名称")
    private String modelName;
}
