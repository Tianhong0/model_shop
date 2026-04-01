package org.majun.backend.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.majun.backend.annotation.DataScope;
import org.majun.backend.annotation.DataScopes;
import org.majun.backend.context.DataScopeContext;
import org.majun.backend.entity.SysRole;
import org.majun.backend.enums.DataScopeType;
import org.majun.backend.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 数据权限切面
 * 在方法执行前设置数据权限上下文，执行后清除
 */
@Slf4j
@Aspect
@Component
public class DataScopeAspect {

    @Around("@annotation(org.majun.backend.annotation.DataScope) || " +
            "@annotation(org.majun.backend.annotation.DataScopes) || " +
            "@within(org.majun.backend.annotation.DataScope)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        try {
            // 设置数据权限上下文
            setupDataScopeContext(point);

            // 执行方法
            return point.proceed();
        } finally {
            // 清除上下文
            DataScopeContext.clear();
        }
    }

    /**
     * 设置数据权限上下文
     */
    private void setupDataScopeContext(ProceedingJoinPoint point) {
        // 获取当前用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            return;
        }

        Long userId = loginUser.getId();
        List<SysRole> roles = loginUser.getRoles();

        if (roles == null || roles.isEmpty()) {
            return;
        }

        // 获取数据范围类型（取角色中最大的权限）
        DataScopeType scopeType = getDataScopeType(roles);

        // 判断是否跳过检查（管理员）
        boolean skipCheck = isAdmin(roles);

        // 获取注解信息
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        DataScope dataScope = method.getAnnotation(DataScope.class);
        if (dataScope == null) {
            DataScopes dataScopes = method.getAnnotation(DataScopes.class);
            if (dataScopes != null && dataScopes.value().length > 0) {
                dataScope = dataScopes.value()[0];
            }
        }

        // 构建上下文信息
        DataScopeContext.DataScopeInfo info = DataScopeContext.DataScopeInfo.builder()
                .userId(userId)
                .scopeType(scopeType)
                .skipCheck(skipCheck)
                .build();

        if (dataScope != null) {
            info.setResourceType(dataScope.resourceType());
            info.setOwnerField(dataScope.ownerField());
            info.setTableAlias(dataScope.tableAlias());
        }

        DataScopeContext.set(info);
    }

    /**
     * 获取数据范围类型（取角色中权限最大的）
     */
    private DataScopeType getDataScopeType(List<SysRole> roles) {
        DataScopeType result = DataScopeType.SELF;
        for (SysRole role : roles) {
            DataScopeType type = DataScopeType.fromCode(role.getDataScope());
            // ALL > SELF
            if (type.getCode() < result.getCode()) {
                result = type;
            }
        }
        return result;
    }

    /**
     * 判断是否为管理员
     */
    private boolean isAdmin(List<SysRole> roles) {
        return roles.stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getRoleName()));
    }
}
