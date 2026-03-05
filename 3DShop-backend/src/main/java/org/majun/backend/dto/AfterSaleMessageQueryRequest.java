package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "After-sale message query request")
public class AfterSaleMessageQueryRequest {

    @NotNull(message = "After-sale ID is required")
    private Long afterSaleId;

    @Min(value = 1, message = "Page number must be >= 1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "Page size must be >= 1")
    private Integer pageSize = 20;
}
