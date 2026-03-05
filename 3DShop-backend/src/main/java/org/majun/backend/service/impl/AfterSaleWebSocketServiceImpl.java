package org.majun.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.service.AfterSaleWebSocketService;
import org.majun.backend.vo.AfterSaleMessageVO;
import org.majun.backend.websocket.AfterSaleWebSocketSessionStore;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
@RequiredArgsConstructor
@Slf4j
public class AfterSaleWebSocketServiceImpl implements AfterSaleWebSocketService, AfterSaleWebSocketSessionStore {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, CopyOnWriteArraySet<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    @Override
    public void broadcastMessage(Long afterSaleId, AfterSaleMessageVO message) {
        if (afterSaleId == null || message == null) {
            return;
        }
        Set<WebSocketSession> sessions = roomSessions.get(afterSaleId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String text;
        try {
            var payload = new java.util.HashMap<String, Object>();
            payload.put("eventType", "NEW_MESSAGE");
            payload.put("afterSaleId", afterSaleId);
            payload.put("message", message);
            text = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.warn("序列化售后WS消息失败 afterSaleId={}", afterSaleId, ex);
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
                log.warn("推送售后WS消息失败 sessionId={}, afterSaleId={}", session.getId(), afterSaleId, ex);
            }
        }
    }

    @Override
    public void add(Long afterSaleId, WebSocketSession session) {
        if (afterSaleId == null || session == null) {
            return;
        }
        roomSessions.computeIfAbsent(afterSaleId, key -> new CopyOnWriteArraySet<>()).add(session);
    }

    @Override
    public void remove(Long afterSaleId, WebSocketSession session) {
        if (afterSaleId == null || session == null) {
            return;
        }
        CopyOnWriteArraySet<WebSocketSession> sessions = roomSessions.get(afterSaleId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            roomSessions.remove(afterSaleId);
        }
    }
}
