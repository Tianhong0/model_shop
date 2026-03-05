package org.majun.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.ResultCode;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.config.JwtProperties;
import org.majun.backend.util.JwtUtil;
import org.majun.backend.util.RedisUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

/**
 * JWT认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final JwtProperties jwtProperties;
    private final RedisUtil redisUtil;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        // 获取Token
        String token = getToken(request);

        // 如果Token存在，进行验证
        if (StringUtils.hasText(token)) {
            try {
                // 校验 JWT 本身
                if (!jwtUtil.validateToken(token)) {
                    throw new BusinessException(ResultCode.TOKEN_INVALID);
                }

                // Redis 作为硬失败条件，并做双向一致性校验
                Long userId = jwtUtil.getUserId(token);
                if (!redisUtil.isTokenValid(token, userId)) {
                    log.warn("Token未通过Redis一致性校验, uri={}, userId={}", request.getRequestURI(), userId);
                    throw new BusinessException(ResultCode.TOKEN_INVALID);
                }

                // 获取用户名
                String username = jwtUtil.getUserName(token);

                // 从缓存中获取用户信息（实现Redis缓存用户详情）
                LoginUser loginUser = getLoginUserFromCache(username);
                if (loginUser == null) {
                    // 如果缓存中没有，从数据库加载并缓存
                    loginUser = (LoginUser) userDetailsService.loadUserByUsername(username);
                    cacheLoginUser(loginUser);
                }

                // 设置认证信息
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 放入Security上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("用户{}认证成功", username);
            } catch (BusinessException e) {
                log.warn("Token认证失败: {}", e.getMessage());
            } catch (Exception e) {
                log.error("Token解析异常", e);
            }
        }

        // 继续执行过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 从缓存中获取登录用户
     */
    private LoginUser getLoginUserFromCache(String username) {
        String cacheKey = "user:details:" + username;
        return redisUtil.getLoginUser(cacheKey);
    }

    /**
     * 缓存登录用户信息
     */
    private void cacheLoginUser(LoginUser loginUser) {
        String cacheKey = "user:details:" + loginUser.getUsername();
        redisUtil.setLoginUser(cacheKey, loginUser, jwtProperties.getExpiration());
    }

    /**
     * 从请求中获取Token
     */
    private String getToken(HttpServletRequest request) {
        String authHeader = request.getHeader(jwtProperties.getHeader());
        if (!StringUtils.hasText(authHeader)) {
            return null;
        }

        String trimmed = authHeader.trim();
        String configuredPrefix = jwtProperties.getPrefix();

        if (StringUtils.hasText(configuredPrefix) && trimmed.startsWith(configuredPrefix)) {
            return trimmed.substring(configuredPrefix.length()).trim();
        }

        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("bearer ")) {
            return trimmed.substring(7).trim();
        }

        // 兼容直接传裸 token 的场景
        if (!trimmed.contains(" ")) {
            return trimmed;
        }

        return null;
    }
}
