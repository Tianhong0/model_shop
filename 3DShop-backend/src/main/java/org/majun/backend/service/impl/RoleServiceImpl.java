package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.dto.AssignRoleRequest;
import org.majun.backend.dto.RoleCreateRequest;
import org.majun.backend.dto.RoleQueryRequest;
import org.majun.backend.dto.RoleUpdateRequest;
import org.majun.backend.entity.SysPermission;
import org.majun.backend.entity.SysRole;
import org.majun.backend.entity.SysRolePermission;
import org.majun.backend.entity.SysUserRole;
import org.majun.backend.repository.SysPermissionRepository;
import org.majun.backend.repository.SysRolePermissionRepository;
import org.majun.backend.repository.SysRoleRepository;
import org.majun.backend.repository.SysUserRoleRepository;
import org.majun.backend.service.RoleService;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.PermissionVO;
import org.majun.backend.vo.RoleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 角色管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
/**
 * 角色服务实现
 */
public class RoleServiceImpl implements RoleService {

    private final SysRoleRepository roleRepository;
    private final SysPermissionRepository permissionRepository;
    private final SysRolePermissionRepository rolePermissionRepository;
    private final SysUserRoleRepository userRoleRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRole(RoleCreateRequest request) {
        // 检查角色名是否已存在
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getRoleName, request.getRoleName());
        if (roleRepository.selectCount(wrapper) > 0) {
            throw new BusinessException("角色名称已存在");
        }

        SysRole role = new SysRole();
        role.setRoleName(request.getRoleName());
        role.setRoleDesc(request.getRoleDesc());
        role.setStatus(1);
        roleRepository.insert(role);

        // 分配权限
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            assignPermissions(role.getId(), request.getPermissionIds());
        }

        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(RoleUpdateRequest request) {
        SysRole role = roleRepository.selectById(request.getId());
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        // 检查角色名是否重复
        if (StringUtils.hasText(request.getRoleName()) && !request.getRoleName().equals(role.getRoleName())) {
            LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysRole::getRoleName, request.getRoleName())
                    .ne(SysRole::getId, request.getId());
            if (roleRepository.selectCount(wrapper) > 0) {
                throw new BusinessException("角色名称已存在");
            }
            role.setRoleName(request.getRoleName());
        }

        if (request.getRoleDesc() != null) {
            role.setRoleDesc(request.getRoleDesc());
        }
        if (request.getStatus() != null) {
            role.setStatus(request.getStatus());
        }
        roleRepository.updateById(role);

        // 更新权限
        if (request.getPermissionIds() != null) {
            // 删除原权限
            LambdaQueryWrapper<SysRolePermission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysRolePermission::getRoleId, request.getId());
            rolePermissionRepository.delete(wrapper);
            // 分配新权限
            if (!request.getPermissionIds().isEmpty()) {
                assignPermissions(request.getId(), request.getPermissionIds());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        SysRole role = roleRepository.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        // 检查是否有用户使用该角色
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getRoleId, id);
        if (userRoleRepository.selectCount(wrapper) > 0) {
            throw new BusinessException("该角色已分配给用户，无法删除");
        }

        // 删除角色权限关联
        LambdaQueryWrapper<SysRolePermission> rpWrapper = new LambdaQueryWrapper<>();
        rpWrapper.eq(SysRolePermission::getRoleId, id);
        rolePermissionRepository.delete(rpWrapper);

        // 删除角色
        roleRepository.deleteById(id);
    }

    @Override
    public RoleVO getRoleDetail(Long id) {
        SysRole role = roleRepository.selectById(id);
        if (role == null) {
            return null;
        }
        return toRoleVO(role, true);
    }

    @Override
    public PageResult<RoleVO> queryRoles(RoleQueryRequest request) {
        Page<SysRole> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(request.getRoleName())) {
            wrapper.like(SysRole::getRoleName, request.getRoleName());
        }
        if (request.getStatus() != null) {
            wrapper.eq(SysRole::getStatus, request.getStatus());
        }
        wrapper.orderByDesc(SysRole::getCreateTime);

        Page<SysRole> result = roleRepository.selectPage(page, wrapper);

        List<RoleVO> voList = result.getRecords().stream()
                .map(role -> toRoleVO(role, false))
                .toList();

        int pages = (int) ((result.getTotal() + result.getSize() - 1) / result.getSize());

        return PageResult.<RoleVO>builder()
                .records(voList)
                .total(result.getTotal())
                .pageNum((int) result.getCurrent())
                .pageSize((int) result.getSize())
                .pages(pages)
                .build();
    }

    @Override
    public List<RoleVO> getAllEnabledRoles() {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getCreateTime);
        return roleRepository.selectList(wrapper).stream()
                .map(role -> toRoleVO(role, false))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignUserRoles(AssignRoleRequest request) {
        // 删除用户原有角色
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, request.getUserId());
        userRoleRepository.delete(wrapper);

        // 分配新角色
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            for (Long roleId : request.getRoleIds()) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(request.getUserId());
                userRole.setRoleId(roleId);
                userRoleRepository.insert(userRole);
            }
        }
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId);
        return userRoleRepository.selectList(wrapper).stream()
                .map(SysUserRole::getRoleId)
                .toList();
    }

    @Override
    public List<PermissionVO> getAllPermissions() {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getStatus, 1)
                .orderByAsc(SysPermission::getSortOrder);
        List<SysPermission> permissions = permissionRepository.selectList(wrapper);
        return buildPermissionTree(permissions, 0L);
    }

    @Override
    public List<Long> getRolePermissionIds(Long roleId) {
        LambdaQueryWrapper<SysRolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRolePermission::getRoleId, roleId);
        return rolePermissionRepository.selectList(wrapper).stream()
                .map(SysRolePermission::getPermissionId)
                .toList();
    }

    @Override
    public List<String> getUserPermissionCodes(Long userId) {
        // 1. 获取用户所有角色ID
        List<Long> roleIds = getUserRoleIds(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 获取角色关联的所有权限ID
        LambdaQueryWrapper<SysRolePermission> rpWrapper = new LambdaQueryWrapper<>();
        rpWrapper.in(SysRolePermission::getRoleId, roleIds);
        List<Long> permissionIds = rolePermissionRepository.selectList(rpWrapper).stream()
                .map(SysRolePermission::getPermissionId)
                .distinct()
                .toList();

        if (permissionIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 获取权限详情，提取权限码
        LambdaQueryWrapper<SysPermission> pWrapper = new LambdaQueryWrapper<>();
        pWrapper.in(SysPermission::getId, permissionIds)
                .eq(SysPermission::getStatus, 1);
        return permissionRepository.selectList(pWrapper).stream()
                .map(SysPermission::getPermissionCode)
                .toList();
    }

    @Override
    public List<PermissionVO> getUserMenus(Long userId) {
        // 1. 获取用户所有角色ID
        List<Long> roleIds = getUserRoleIds(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 获取角色关联的所有权限ID
        LambdaQueryWrapper<SysRolePermission> rpWrapper = new LambdaQueryWrapper<>();
        rpWrapper.in(SysRolePermission::getRoleId, roleIds);
        List<Long> permissionIds = rolePermissionRepository.selectList(rpWrapper).stream()
                .map(SysRolePermission::getPermissionId)
                .distinct()
                .toList();

        if (permissionIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 查询所有启用的菜单类型权限
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getStatus, 1)
                .eq(SysPermission::getPermissionType, "MENU")
                .in(SysPermission::getId, permissionIds)
                .orderByAsc(SysPermission::getSortOrder);
        List<SysPermission> menus = permissionRepository.selectList(wrapper);

        // 4. 构建菜单树
        return buildPermissionTree(menus, 0L);
    }

    private void assignPermissions(Long roleId, List<Long> permissionIds) {
        for (Long permissionId : permissionIds) {
            SysRolePermission rp = new SysRolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(permissionId);
            rolePermissionRepository.insert(rp);
        }
    }

    private RoleVO toRoleVO(SysRole role, boolean withPermissions) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleName(role.getRoleName());
        vo.setRoleDesc(role.getRoleDesc());
        vo.setStatus(role.getStatus());
        vo.setCreateTime(role.getCreateTime());
        vo.setUpdateTime(role.getUpdateTime());

        if (withPermissions) {
            vo.setPermissionIds(getRolePermissionIds(role.getId()));
        }
        return vo;
    }

    private List<PermissionVO> buildPermissionTree(List<SysPermission> permissions, Long parentId) {
        List<PermissionVO> tree = new ArrayList<>();
        for (SysPermission permission : permissions) {
            if (parentId.equals(permission.getParentId())) {
                PermissionVO vo = toPermissionVO(permission);
                vo.setChildren(buildPermissionTree(permissions, permission.getId()));
                tree.add(vo);
            }
        }
        return tree;
    }

    private PermissionVO toPermissionVO(SysPermission permission) {
        PermissionVO vo = new PermissionVO();
        vo.setId(permission.getId());
        vo.setPermissionCode(permission.getPermissionCode());
        vo.setPermissionName(permission.getPermissionName());
        vo.setPermissionType(permission.getPermissionType());
        vo.setParentId(permission.getParentId());
        vo.setMenuPath(permission.getMenuPath());
        vo.setIcon(permission.getIcon());
        vo.setSortOrder(permission.getSortOrder());
        vo.setStatus(permission.getStatus());
        vo.setCreateTime(permission.getCreateTime());
        return vo;
    }
}
