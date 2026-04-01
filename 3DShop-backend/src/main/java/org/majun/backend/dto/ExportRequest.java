package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 通用数据导出请求
 */
@Data
@Schema(description = "数据导出请求")
public class ExportRequest {

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "导出格式: xlsx/csv")
    private String format = "xlsx";

    @Schema(description = "指定导出的字段（为空则导出全部）")
    private List<String> fields;
}
