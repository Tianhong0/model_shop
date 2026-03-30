package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "模型清单状态更新请求")
public class ModelListStatusUpdateRequest {

    @NotNull(message = "清单ID不能为空")
    @Schema(description = "清单ID")
    private Long listId;

    @NotNull(message = "状态不能为空")
    @Schema(description = "目标状态: 1-发布, 2-下架")
    private Integer status;
}
