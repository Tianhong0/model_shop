package org.majun.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.service.PrintWebSocketService;
import org.majun.backend.vo.PrintJobProgressVO;
import org.majun.backend.websocket.PrintWebSocketSessionStore;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrintWebSocketServiceImpl implements PrintWebSocketService, PrintWebSocketSessionStore {

    private final ObjectMapper objectMapper;
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void broadcast(PrintJobProgressVO payload) {
        if (sessions.isEmpty()) {
            return;
        }
        String text;
        try {
            text = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.warn("打印进度序列化失败", ex);
            return;
        }

        TextMessage message = new TextMessage(text);
        for (WebSocketSession session : sessions) {
            if (session == null || !session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(message);
            } catch (Exception ex) {
                log.warn("推送打印进度失败 session={}", session.getId(), ex);
            }
        }
    }

    @Override
    public void add(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void remove(WebSocketSession session) {
        sessions.remove(session);
    }
}
