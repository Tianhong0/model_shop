package org.majun.backend.websocket;

import org.springframework.web.socket.WebSocketSession;

public interface PrintWebSocketSessionStore {
    void add(WebSocketSession session);

    void remove(WebSocketSession session);
}
