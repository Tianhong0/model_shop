package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "更新模型清单请求")
public class ModelListUpdateRequest {

    @NotNull(message = "清单ID不能为空")
    @Schema(description = "清单ID")
    private Long id;

    @Schema(description = "清单标题")
    private String title;

    @Schema(description = "清单描述")
    private String description;

    @Schema(description = "封面图URL")
    private String coverImage;
}
