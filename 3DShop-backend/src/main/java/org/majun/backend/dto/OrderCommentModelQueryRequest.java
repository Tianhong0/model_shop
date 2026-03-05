package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Model comment query request")
public class OrderCommentModelQueryRequest {

    @NotNull(message = "Model ID is required")
    @Schema(description = "Model ID")
    private Long modelId;

    @Min(value = 1, message = "Page number must be >= 1")
    @Schema(description = "Page number")
    private Integer pageNum = 1;

    @Min(value = 1, message = "Page size must be >= 1")
    @Schema(description = "Page size")
    private Integer pageSize = 10;

    @Schema(description = "排序方式: hot(点赞优先)/latest(最新)")
    private String sortType = "latest";
}
