package org.majun.backend.websocket;

import org.springframework.web.socket.WebSocketSession;

/**
 * 售后消息 WebSocket 会话存储接口
 */
public interface AfterSaleWebSocketSessionStore {

    void add(Long afterSaleId, WebSocketSession session);

    void remove(Long afterSaleId, WebSocketSession session);
}
