package org.majun.backend.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.entity.UsedListing;
import org.majun.backend.repository.UsedListingRepository;
import org.majun.backend.service.UsedMessageWebSocketService;
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
public class UsedMessageWebSocketHandler extends TextWebSocketHandler {

    private static final String ATTR_ROOM_KEY = "roomKey";

    private final JwtUtil jwtUtil;
    private final UsedListingRepository usedListingRepository;
    private final UsedMessageWebSocketService usedMessageWebSocketService;

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
            Long listingId = parseLong(params.get("listingId"));
            Long counterpartId = parseLong(params.get("counterpartId"));
            if (userId == null || listingId == null || counterpartId == null) {
                closeUnauthorized(session, "参数无效");
                return;
            }

            UsedListing listing = usedListingRepository.selectById(listingId);
            if (listing == null || Objects.equals(listing.getIsDelete(), 1)) {
                closeUnauthorized(session, "商品不存在");
                return;
            }
            if (!Objects.equals(userId, listing.getSellerId()) && !Objects.equals(counterpartId, listing.getSellerId())) {
                closeUnauthorized(session, "仅允许买家与卖家会话");
                return;
            }
            if (Objects.equals(userId, counterpartId)) {
                closeUnauthorized(session, "会话对象无效");
                return;
            }

            String roomKey = buildRoomKey(listingId, userId, counterpartId);
            session.getAttributes().put(ATTR_ROOM_KEY, roomKey);
            if (usedMessageWebSocketService instanceof UsedMessageWebSocketSessionStore store) {
                store.add(roomKey, session);
            }
        } catch (Exception ex) {
            log.warn("建立二手消息WS连接失败", ex);
            closeUnauthorized(session, "连接鉴权失败");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object roomKey = session.getAttributes().get(ATTR_ROOM_KEY);
        if (roomKey instanceof String key && usedMessageWebSocketService instanceof UsedMessageWebSocketSessionStore store) {
            store.remove(key, session);
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
        String token = queryParams.get("token");
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return token.startsWith("Bearer ") ? token.substring(7) : token;
    }

    private Long parseLong(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String buildRoomKey(Long listingId, Long userId, Long counterpartId) {
        long min = Math.min(userId, counterpartId);
        long max = Math.max(userId, counterpartId);
        return listingId + ":" + min + ":" + max;
    }

    private void closeUnauthorized(WebSocketSession session, String reason) throws IOException {
        if (session != null && session.isOpen()) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason(reason));
        }
    }
}
