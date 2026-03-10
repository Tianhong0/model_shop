package org.majun.backend.websocket;

import org.springframework.web.socket.WebSocketSession;

public interface UsedMessageWebSocketSessionStore {

    void add(String roomKey, WebSocketSession session);

    void remove(String roomKey, WebSocketSession session);
}
