package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 轮播图状态更新请求
 */
@Data
@Schema(description = "轮播图状态更新请求")
public class BannerStatusUpdateRequest {

    @NotNull(message = "ID不能为空")
    @Schema(description = "ID")
    private Long id;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态: 1-启用, 0-禁用")
    private Integer status;
}
