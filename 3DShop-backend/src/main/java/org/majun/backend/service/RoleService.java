package org.majun.backend.service;

import org.majun.backend.dto.AssignRoleRequest;
import org.majun.backend.dto.RoleCreateRequest;
import org.majun.backend.dto.RoleQueryRequest;
import org.majun.backend.dto.RoleUpdateRequest;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.PermissionVO;
import org.majun.backend.vo.RoleVO;

import java.util.List;

/**
 * 角色管理服务接口
 */
public interface RoleService {

    /**
     * 创建角色
     */
    Long createRole(RoleCreateRequest request);

    /**
     * 更新角色
     */
    void updateRole(RoleUpdateRequest request);

    /**
     * 删除角色
     */
    void deleteRole(Long id);

    /**
     * 获取角色详情
     */
    RoleVO getRoleDetail(Long id);

    /**
     * 分页查询角色
     */
    PageResult<RoleVO> queryRoles(RoleQueryRequest request);

    /**
     * 获取所有启用的角色
     */
    List<RoleVO> getAllEnabledRoles();

    /**
     * 分配用户角色
     */
    void assignUserRoles(AssignRoleRequest request);

    /**
     * 获取用户的角色ID列表
     */
    List<Long> getUserRoleIds(Long userId);

    /**
     * 获取所有权限列表（树形结构）
     */
    List<PermissionVO> getAllPermissions();

    /**
     * 获取角色的权限ID列表
     */
    List<Long> getRolePermissionIds(Long roleId);

    /**
     * 获取用户的所有权限编码列表
     * @param userId 用户ID
     * @return 权限编码列表
     */
    List<String> getUserPermissionCodes(Long userId);

    /**
     * 获取用户的菜单树
     * @param userId 用户ID
     * @return 菜单树
     */
    List<PermissionVO> getUserMenus(Long userId);
}
