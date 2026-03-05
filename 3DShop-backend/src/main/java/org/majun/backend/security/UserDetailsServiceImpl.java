package org.majun.backend.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.common.ResultCode;
import org.majun.backend.entity.SysRole;
import org.majun.backend.entity.SysUser;
import org.majun.backend.entity.SysUserRole;
import org.majun.backend.repository.SysRoleRepository;
import org.majun.backend.repository.SysUserRepository;
import org.majun.backend.repository.SysUserRoleRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户详情服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserRepository userRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysRoleRepository roleRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 根据用户名查询用户
        SysUser user = userRepository.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUserName, username)
        );

        if (user == null) {
            log.info("用户名不存在: {}", username);
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (user.getStatus() == 0) {
            log.info("用户已被禁用: {}", username);
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        if (user.getIsDelete() == 1) {
            log.info("用户已被删除: {}", username);
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 查询用户角色
        List<SysUserRole> userRoles = userRoleRepository.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, user.getId())
        );

        List<SysRole> roles = userRoles.stream()
                .map(userRole -> roleRepository.selectById(userRole.getRoleId()))
                .filter(role -> role != null && role.getStatus() == 1)
                .collect(Collectors.toList());

        // 创建包含角色信息的LoginUser
        return new LoginUser(user, roles);
    }
}
