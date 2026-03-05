package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 公告状态更新请求
 */
@Data
@Schema(description = "公告状态更新请求")
public class NoticeStatusUpdateRequest {

    @NotNull(message = "ID不能为空")
    @Schema(description = "ID")
    private Long id;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态: 1-发布, 0-草稿")
    private Integer status;
}
