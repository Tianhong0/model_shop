package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Order batch app pay create request")
public class OrderBatchPayCreateRequest {

    @NotEmpty(message = "Order IDs are required")
    @Schema(description = "Order IDs")
    private List<Long> orderIds;
}
