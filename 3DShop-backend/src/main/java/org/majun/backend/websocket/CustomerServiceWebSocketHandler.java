package org.majun.backend.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.service.CustomerServiceWebSocketService;
import org.majun.backend.util.JwtUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerServiceWebSocketHandler extends TextWebSocketHandler {

    private static final String ATTR_USER_ID = "userId";
    private static final String ATTR_USER_ROLE = "userRole";
    private static final String ATTR_CONVERSATION_ID = "conversationId";

    private final JwtUtil jwtUtil;
    private final CustomerServiceWebSocketService customerServiceWebSocketService;

    // 会话ID -> 参与的Session集合
    private final ConcurrentHashMap<Long, CopyOnWriteArraySet<WebSocketSession>> conversationSessions = new ConcurrentHashMap<>();

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
            String userRole = params.getOrDefault("userRole", "USER");
            Long conversationId = params.get("conversationId") != null ? parseLong(params.get("conversationId")) : null;

            session.getAttributes().put(ATTR_USER_ID, userId);
            session.getAttributes().put(ATTR_USER_ROLE, userRole);
            if (conversationId != null) {
                session.getAttributes().put(ATTR_CONVERSATION_ID, conversationId);
            }

            // 注册session
            if (conversationId != null) {
                conversationSessions.computeIfAbsent(conversationId, k -> new CopyOnWriteArraySet<>()).add(session);
            }

            // 用户注册到WebSocketService（用于消息推送）
            if ("USER".equals(userRole) && conversationId != null) {
                customerServiceWebSocketService.registerUserSession(conversationId, session);
            }

            // 客服注册到WebSocketService并更新心跳
            if ("ADMIN".equals(userRole)) {
                customerServiceWebSocketService.registerAdminSession(userId, session);
                customerServiceWebSocketService.updateAdminHeartbeat(userId);
            }

        } catch (Exception ex) {
            log.warn("建立客服WS连接失败", ex);
            closeUnauthorized(session, "连接鉴权失败");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);
        String userRole = (String) session.getAttributes().get(ATTR_USER_ROLE);
        Long conversationId = (Long) session.getAttributes().get(ATTR_CONVERSATION_ID);

        try {
            String payload = message.getPayload();
            log.debug("收到客服WS消息: {}", payload);

            // 简单的心跳处理
            if ("ping".equals(payload)) {
                if ("ADMIN".equals(userRole)) {
                    customerServiceWebSocketService.updateAdminHeartbeat(userId);
                }
                session.sendMessage(new TextMessage("pong"));
            }
        } catch (Exception e) {
            log.error("处理客服WS消息失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);
        String userRole = (String) session.getAttributes().get(ATTR_USER_ROLE);
        Long conversationId = (Long) session.getAttributes().get(ATTR_CONVERSATION_ID);

        if (conversationId != null) {
            CopyOnWriteArraySet<WebSocketSession> sessions = conversationSessions.get(conversationId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    conversationSessions.remove(conversationId);
                }
            }
        }

        // 清理 WebSocketService 中的用户 session
        if ("USER".equals(userRole) && conversationId != null) {
            customerServiceWebSocketService.unregisterUserSession(conversationId, session);
        }

        // 清理 WebSocketService 中的客服 session
        if ("ADMIN".equals(userRole) && userId != null) {
            customerServiceWebSocketService.unregisterAdminSession(userId, session);
        }


    }

    private Map<String, String> parseQueryParams(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query == null || query.isEmpty()) {
            return Map.of();
        }
        return java.util.Arrays.stream(query.split("&"))
            .map(s -> s.split("=", 2))
            .filter(arr -> arr.length == 2)
            .collect(java.util.stream.Collectors.toMap(
                arr -> arr[0],
                arr -> java.net.URLDecoder.decode(arr[1], java.nio.charset.StandardCharsets.UTF_8)
            ));
    }

    private String resolveToken(Map<String, String> params) {
        String token = params.get("token");
        if (token != null) {
            try {
                token = java.net.URLDecoder.decode(token, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                // ignore
            }
        }
        return token;
    }

    private Long parseLong(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void closeUnauthorized(WebSocketSession session, String reason) {
        try {
            session.sendMessage(new TextMessage("{\"error\":\"" + reason + "\"}"));
            session.close();
        } catch (IOException e) {
            log.warn("关闭未授权连接失败", e);
        }
    }
}
