package org.majun.backend.websocket;

import org.springframework.web.socket.WebSocketSession;

public interface BountyWebSocketSessionStore {

    void add(Long taskId, WebSocketSession session);

    void remove(Long taskId, WebSocketSession session);
}
