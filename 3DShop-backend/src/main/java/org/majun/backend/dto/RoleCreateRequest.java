package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 角色创建请求
 */
@Data
@Schema(description = "角色创建请求")
public class RoleCreateRequest {

    @NotBlank(message = "角色名称不能为空")
    @Schema(description = "角色名称，如：ROLE_MANAGER")
    private String roleName;

    @Schema(description = "角色描述")
    private String roleDesc;

    @Schema(description = "权限ID列表")
    private List<Long> permissionIds;
}
