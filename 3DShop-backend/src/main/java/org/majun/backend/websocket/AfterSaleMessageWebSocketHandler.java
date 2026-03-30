package org.majun.backend.websocket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.entity.SysOrderAfterSale;
import org.majun.backend.entity.SysRole;
import org.majun.backend.entity.SysUserRole;
import org.majun.backend.repository.SysOrderAfterSaleRepository;
import org.majun.backend.repository.SysRoleRepository;
import org.majun.backend.repository.SysUserRoleRepository;
import org.majun.backend.service.AfterSaleWebSocketService;
import org.majun.backend.util.JwtUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AfterSaleMessageWebSocketHandler extends TextWebSocketHandler {

    private static final String ATTR_AFTER_SALE_ID = "afterSaleId";

    private final AfterSaleWebSocketService afterSaleWebSocketService;
    private final JwtUtil jwtUtil;
    private final SysOrderAfterSaleRepository afterSaleRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysRoleRepository roleRepository;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            Map<String, String> params = parseQueryParams(session);
            String token = resolveToken(params);
            if (!StringUtils.hasText(token) || !jwtUtil.validateToken(token)) {
                closeUnauthorized(session, "token无效");
                return;
            }

            Long userId = jwtUtil.getUserId(token);
            if (userId == null) {
                closeUnauthorized(session, "用户信息无效");
                return;
            }

            Long afterSaleId = parseAfterSaleId(params.get("afterSaleId"));
            if (afterSaleId == null) {
                closeUnauthorized(session, "afterSaleId无效");
                return;
            }

            SysOrderAfterSale afterSale = afterSaleRepository.selectById(afterSaleId);
            if (afterSale == null || Objects.equals(afterSale.getIsDelete(), 1)) {
                closeUnauthorized(session, "售后单不存在");
                return;
            }

            boolean isAdmin = hasAdminRole(userId);
            if (!isAdmin && !Objects.equals(afterSale.getUserId(), userId)) {
                closeUnauthorized(session, "无权限加入该会话");
                return;
            }

            session.getAttributes().put(ATTR_AFTER_SALE_ID, afterSaleId);
            if (afterSaleWebSocketService instanceof AfterSaleWebSocketSessionStore store) {
                store.add(afterSaleId, session);
            }
        } catch (Exception ex) {
            log.warn("建立售后WS连接失败", ex);
            closeUnauthorized(session, "连接鉴权失败");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object afterSaleIdObj = session.getAttributes().get(ATTR_AFTER_SALE_ID);
        if (afterSaleIdObj instanceof Long afterSaleId && afterSaleWebSocketService instanceof AfterSaleWebSocketSessionStore store) {
            store.remove(afterSaleId, session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    }

    private Map<String, String> parseQueryParams(WebSocketSession session) {
        if (session.getUri() == null || !StringUtils.hasText(session.getUri().getQuery())) {
            return Collections.emptyMap();
        }
        String[] pairs = session.getUri().getQuery().split("&");
        Map<String, String> result = new HashMap<>();
        for (String pair : pairs) {
            if (!StringUtils.hasText(pair)) {
                continue;
            }
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            result.put(key, value);
        }
        return result;
    }

    private String resolveToken(Map<String, String> queryParams) {
        if (queryParams == null) {
            return null;
        }
        String token = queryParams.get("token");
        if (!StringUtils.hasText(token)) {
            return null;
        }
        if (token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    private Long parseAfterSaleId(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean hasAdminRole(Long userId) {
        List<SysUserRole> userRoles = userRoleRepository.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
        if (userRoles == null || userRoles.isEmpty()) {
            return false;
        }
        Set<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
        if (roleIds.isEmpty()) {
            return false;
        }
        List<SysRole> roles = roleRepository.selectBatchIds(roleIds);
        return roles != null && roles.stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getRoleName()));
    }

    private void closeUnauthorized(WebSocketSession session, String reason) throws IOException {
        if (session != null && session.isOpen()) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason(reason));
        }
    }
}
