package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色查询请求
 */
@Data
@Schema(description = "角色查询请求")
public class RoleQueryRequest {

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "角色名称(模糊查询)")
    private String roleName;

    @Schema(description = "状态：1-启用, 0-禁用")
    private Integer status;
}
