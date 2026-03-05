package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "积分流水分页查询")
public class PointLedgerQueryRequest {

    @Min(value = 1, message = "页码必须大于等于1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页大小必须大于等于1")
    private Integer pageSize = 10;

    @Schema(description = "方向:1收入,2支出")
    private Integer direction;

    @Schema(description = "业务类型")
    private String bizType;
}
