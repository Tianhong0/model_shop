package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "After-sale query request")
public class AfterSaleQueryRequest {

    @Min(value = 1, message = "Page number must be >= 1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "Page size must be >= 1")
    private Integer pageSize = 10;

    @Schema(description = "After-sale status")
    private Integer status;

    @Schema(description = "After-sale type")
    private Integer type;

    @Schema(description = "Order serial number")
    private String orderSn;

    @Schema(description = "After-sale serial number")
    private String afterSaleSn;
}
