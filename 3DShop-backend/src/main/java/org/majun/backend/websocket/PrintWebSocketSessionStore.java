package org.majun.backend.websocket;

import org.springframework.web.socket.WebSocketSession;

/**
 * 打印进度 WebSocket 会话存储接口
 */
public interface PrintWebSocketSessionStore {
    void add(WebSocketSession session);

    void remove(WebSocketSession session);
}
