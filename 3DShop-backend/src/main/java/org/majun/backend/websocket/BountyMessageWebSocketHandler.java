package org.majun.backend.websocket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.entity.BountyBid;
import org.majun.backend.entity.BountyTask;
import org.majun.backend.service.BountyWebSocketService;
import org.majun.backend.repository.BountyBidRepository;
import org.majun.backend.repository.BountyTaskRepository;
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
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class BountyMessageWebSocketHandler extends TextWebSocketHandler {

    private static final String ATTR_TASK_ID = "taskId";

    private final BountyWebSocketService bountyWebSocketService;
    private final JwtUtil jwtUtil;
    private final BountyTaskRepository bountyTaskRepository;
    private final BountyBidRepository bountyBidRepository;

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

            Long taskId = parseTaskId(params.get("taskId"));
            if (taskId == null) {
                closeUnauthorized(session, "taskId无效");
                return;
            }

            BountyTask task = bountyTaskRepository.selectById(taskId);
            if (task == null || Objects.equals(task.getIsDelete(), 1)) {
                closeUnauthorized(session, "任务不存在");
                return;
            }

            if (!canAccessTask(task, userId)) {
                closeUnauthorized(session, "无权限加入该会话");
                return;
            }

            session.getAttributes().put(ATTR_TASK_ID, taskId);
            if (bountyWebSocketService instanceof BountyWebSocketSessionStore store) {
                store.add(taskId, session);
            }
        } catch (Exception ex) {
            log.warn("建立悬赏WS连接失败", ex);
            closeUnauthorized(session, "连接鉴权失败");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object taskIdObj = session.getAttributes().get(ATTR_TASK_ID);
        if (taskIdObj instanceof Long taskId && bountyWebSocketService instanceof BountyWebSocketSessionStore store) {
            store.remove(taskId, session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    }

    private boolean canAccessTask(BountyTask task, Long userId) {
        if (Objects.equals(task.getPublisherId(), userId) || Objects.equals(task.getWinnerDesignerId(), userId)) {
            return true;
        }
        Long count = bountyBidRepository.selectCount(new LambdaQueryWrapper<BountyBid>()
                .eq(BountyBid::getTaskId, task.getId())
                .eq(BountyBid::getDesignerId, userId)
                .eq(BountyBid::getIsDelete, 0));
        return count != null && count > 0;
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

    private Long parseTaskId(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void closeUnauthorized(WebSocketSession session, String reason) throws IOException {
        if (session != null && session.isOpen()) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason(reason));
        }
    }
}
