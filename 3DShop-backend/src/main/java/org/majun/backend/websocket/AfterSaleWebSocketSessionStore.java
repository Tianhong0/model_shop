package org.majun.backend.websocket;

import org.springframework.web.socket.WebSocketSession;

public interface AfterSaleWebSocketSessionStore {

    void add(Long afterSaleId, WebSocketSession session);

    void remove(Long afterSaleId, WebSocketSession session);
}
