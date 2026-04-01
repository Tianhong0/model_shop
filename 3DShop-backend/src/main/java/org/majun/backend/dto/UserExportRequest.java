package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户导出请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户导出请求")
public class UserExportRequest extends ExportRequest {

    @Schema(description = "用户状态列表")
    private List<Integer> statuses;

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "是否设计师")
    private Boolean isDesigner;

    @Schema(description = "注册开始日期")
    private LocalDate registerStartDate;

    @Schema(description = "注册结束日期")
    private LocalDate registerEndDate;
}
