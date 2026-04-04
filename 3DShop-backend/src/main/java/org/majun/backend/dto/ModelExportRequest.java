package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 模型导出请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "模型导出请求")
public class ModelExportRequest extends ExportRequest {

    @Schema(description = "指定导出的模型ID列表（优先使用）")
    private List<Long> modelIds;

    @Schema(description = "模型状态列表")
    private List<Integer> statuses;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "设计师ID")
    private Long designerId;

    @Schema(description = "模型名称（模糊搜索）")
    private String modelName;

    @Schema(description = "是否免费")
    private Boolean isFree;
}
