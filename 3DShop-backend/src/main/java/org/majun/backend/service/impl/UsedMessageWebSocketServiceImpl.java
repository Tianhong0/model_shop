package org.majun.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.service.UsedMessageWebSocketService;
import org.majun.backend.vo.UsedMessageVO;
import org.majun.backend.websocket.UsedMessageWebSocketSessionStore;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
@RequiredArgsConstructor
@Slf4j
/**
 * 二手消息 WebSocket 服务实现
 */
public class UsedMessageWebSocketServiceImpl implements UsedMessageWebSocketService, UsedMessageWebSocketSessionStore {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    @Override
    public void broadcastMessage(String roomKey, Long listingId, UsedMessageVO message) {
        if (roomKey == null || message == null) {
            return;
        }
        Set<WebSocketSession> sessions = roomSessions.get(roomKey);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        String text;
        try {
            var payload = new java.util.HashMap<String, Object>();
            payload.put("eventType", "NEW_MESSAGE");
            payload.put("roomKey", roomKey);
            payload.put("listingId", listingId);
            payload.put("message", message);
            text = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.warn("序列化二手消息失败 roomKey={}", roomKey, ex);
            return;
        }
        TextMessage wsMessage = new TextMessage(text);
        for (WebSocketSession session : sessions) {
            if (session == null || !session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(wsMessage);
            } catch (Exception ex) {
                log.warn("推送二手消息失败 sessionId={}, roomKey={}", session.getId(), roomKey, ex);
            }
        }
    }

    @Override
    public void add(String roomKey, WebSocketSession session) {
        if (roomKey == null || session == null) {
            return;
        }
        roomSessions.computeIfAbsent(roomKey, key -> new CopyOnWriteArraySet<>()).add(session);
    }

    @Override
    public void remove(String roomKey, WebSocketSession session) {
        if (roomKey == null || session == null) {
            return;
        }
        CopyOnWriteArraySet<WebSocketSession> sessions = roomSessions.get(roomKey);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            roomSessions.remove(roomKey);
        }
    }
}
