package org.majun.backend.security;

import lombok.Data;
import org.majun.backend.entity.SysRole;
import org.majun.backend.entity.SysUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 登录用户信息
 */
@Data
public class LoginUser implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户信息
     */
    private SysUser user;

    /**
     * Token
     */
    private String token;

    /**
     * 用户角色列表
     */
    private List<SysRole> roles;

    public LoginUser(SysUser user) {
        this.user = user;
    }

    public LoginUser(SysUser user, String token) {
        this.user = user;
        this.token = token;
    }

    public LoginUser(SysUser user, List<SysRole> roles) {
        this.user = user;
        this.roles = roles;
    }

    public LoginUser(SysUser user, List<SysRole> roles, String token) {
        this.user = user;
        this.roles = roles;
        this.token = token;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return user.getUserPwd();
    }

    @Override
    public String getUsername() {
        return user.getUserName();
    }

    /**
     * 获取用户ID
     */
    public Long getId() {
        return user.getId();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == 1;
    }
}
