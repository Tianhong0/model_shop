package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "模型收藏切换请求")
public class ModelFavoriteToggleRequest {

    @NotNull(message = "模型ID不能为空")
    @Schema(description = "模型ID")
    private Long modelId;
}
