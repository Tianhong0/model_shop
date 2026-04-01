package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.majun.backend.annotation.OperationLog;
import org.majun.backend.common.Result;
import org.majun.backend.dto.AssignRoleRequest;
import org.majun.backend.dto.RoleCreateRequest;
import org.majun.backend.dto.RoleQueryRequest;
import org.majun.backend.dto.RoleUpdateRequest;
import org.majun.backend.service.RoleService;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.PermissionVO;
import org.majun.backend.vo.RoleVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 */
@Tag(name = "角色管理", description = "角色管理接口")
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "创建角色", description = "管理员创建新角色")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @OperationLog(type = "CREATE", module = "角色管理", description = "创建角色", targetType = "ROLE")
    @PostMapping
    public Result<Long> createRole(@Valid @RequestBody RoleCreateRequest request) {
        return Result.success(roleService.createRole(request));
    }

    @Operation(summary = "更新角色", description = "管理员更新角色信息")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @OperationLog(type = "UPDATE", module = "角色管理", description = "更新角色", targetType = "ROLE")
    @PutMapping
    public Result<Void> updateRole(@Valid @RequestBody RoleUpdateRequest request) {
        roleService.updateRole(request);
        return Result.success();
    }

    @Operation(summary = "删除角色", description = "管理员删除角色")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @OperationLog(type = "DELETE", module = "角色管理", description = "删除角色", targetType = "ROLE")
    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }

    @Operation(summary = "获取角色详情", description = "管理员获取角色详情")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/{id}")
    public Result<RoleVO> getRoleDetail(@PathVariable Long id) {
        RoleVO detail = roleService.getRoleDetail(id);
        return detail != null ? Result.success(detail) : Result.fail("角色不存在");
    }

    @Operation(summary = "分页查询角色", description = "管理员分页查询角色列表")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/page")
    public Result<PageResult<RoleVO>> queryRoles(@RequestBody(required = false) RoleQueryRequest request) {
        if (request == null) {
            request = new RoleQueryRequest();
        }
        return Result.success(roleService.queryRoles(request));
    }

    @Operation(summary = "获取所有启用的角色", description = "获取所有启用状态的角色列表")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/enabled")
    public Result<List<RoleVO>> getAllEnabledRoles() {
        return Result.success(roleService.getAllEnabledRoles());
    }

    @Operation(summary = "分配用户角色", description = "管理员为用户分配角色")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @OperationLog(type = "UPDATE", module = "角色管理", description = "分配用户角色", targetType = "USER")
    @PostMapping("/assign")
    public Result<Void> assignUserRoles(@Valid @RequestBody AssignRoleRequest request) {
        roleService.assignUserRoles(request);
        return Result.success();
    }

    @Operation(summary = "获取用户的角色", description = "获取用户已分配的角色ID列表")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/user/{userId}")
    public Result<List<Long>> getUserRoleIds(@PathVariable Long userId) {
        return Result.success(roleService.getUserRoleIds(userId));
    }

    @Operation(summary = "获取所有权限", description = "获取权限树形结构")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/permissions")
    public Result<List<PermissionVO>> getAllPermissions() {
        return Result.success(roleService.getAllPermissions());
    }

    @Operation(summary = "获取角色的权限", description = "获取角色已分配的权限ID列表")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/{id}/permissions")
    public Result<List<Long>> getRolePermissionIds(@PathVariable Long id) {
        return Result.success(roleService.getRolePermissionIds(id));
    }
}
