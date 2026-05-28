package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "模型审核记录响应")
public class ModelAuditRecordVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "模型ID")
    private Long modelId;

    @Schema(description = "审核人ID")
    private Long auditBy;

    @Schema(description = "审核人名称")
    private String auditByName;

    @Schema(description = "审核动作: 1-通过, 2-驳回")
    private Integer action;

    @Schema(description = "审核动作描述")
    private String actionDesc;

    @Schema(description = "分润比例")
    private Integer profitShareRatio;

    @Schema(description = "审核备注")
    private String note;

    @Schema(description = "创建时间")
    private String createTime;
}
