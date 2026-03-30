package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 活动状态更新请求
 */
@Data
@Schema(description = "活动状态更新请求")
public class EventStatusUpdateRequest {

    @NotNull(message = "活动ID不能为空")
    @Schema(description = "活动ID")
    private Long id;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态: 0-未开始, 1-报名中, 2-进行中, 3-评审中, 4-已结束")
    private Integer status;
}
