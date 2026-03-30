package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "添加模型到清单请求")
public class ModelListItemAddRequest {

    @NotNull(message = "清单ID不能为空")
    @Schema(description = "清单ID")
    private Long listId;

    @Valid
    @NotEmpty(message = "模型列表不能为空")
    @Schema(description = "模型列表")
    private List<ModelListCreateRequest.ModelListItemEntry> items;
}
