package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "从清单移除模型请求")
public class ModelListItemRemoveRequest {

    @NotNull(message = "清单ID不能为空")
    @Schema(description = "清单ID")
    private Long listId;

    @NotNull(message = "模型ID不能为空")
    @Schema(description = "模型ID")
    private Long modelId;
}
