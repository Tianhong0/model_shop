package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 设计师分润查询请求DTO
 */
@Data
@Schema(description = "设计师分润查询请求")
public class DesignerSettlementQueryRequest {

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页数量", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "设计师ID")
    private Long designerId;

    @Schema(description = "结算状态: 0-待结算, 1-已结算, 2-结算失败")
    private Integer status;
}
