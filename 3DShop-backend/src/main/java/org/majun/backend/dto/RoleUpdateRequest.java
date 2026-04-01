package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色更新请求
 */
@Data
@Schema(description = "角色更新请求")
public class RoleUpdateRequest {

    @NotNull(message = "角色ID不能为空")
    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色描述")
    private String roleDesc;

    @Schema(description = "权限ID列表")
    private List<Long> permissionIds;

    @Schema(description = "状态：1-启用, 0-禁用")
    private Integer status;
}
