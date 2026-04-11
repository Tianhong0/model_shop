package org.majun.backend.websocket;

import org.springframework.web.socket.WebSocketSession;

/**
 * 二手消息 WebSocket 会话存储接口
 */
public interface UsedMessageWebSocketSessionStore {

    void add(String roomKey, WebSocketSession session);

    void remove(String roomKey, WebSocketSession session);
}
