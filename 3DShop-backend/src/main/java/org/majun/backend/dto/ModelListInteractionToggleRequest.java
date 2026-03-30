package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "模型清单交互切换请求")
public class ModelListInteractionToggleRequest {

    @NotNull(message = "清单ID不能为空")
    @Schema(description = "清单ID")
    private Long listId;

    @NotNull(message = "交互类型不能为空")
    @Schema(description = "交互类型: 1-点赞, 2-收藏")
    private Integer interactType;
}
