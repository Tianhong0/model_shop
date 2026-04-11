package org.majun.backend.websocket;

import lombok.RequiredArgsConstructor;
import org.majun.backend.service.PrintWebSocketService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 打印进度 WebSocket 处理器
 * 用于向前端推送打印任务的实时进度
 */
@Component
@RequiredArgsConstructor
public class PrintProgressWebSocketHandler extends TextWebSocketHandler {

    private final PrintWebSocketService printWebSocketService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        if (printWebSocketService instanceof PrintWebSocketSessionStore store) {
            store.add(session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        if (printWebSocketService instanceof PrintWebSocketSessionStore store) {
            store.remove(session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 只推送服务端数据，不处理客户端命令
    }
}
