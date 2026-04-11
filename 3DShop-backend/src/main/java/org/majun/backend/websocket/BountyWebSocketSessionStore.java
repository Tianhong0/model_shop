package org.majun.backend.websocket;

import org.springframework.web.socket.WebSocketSession;

/**
 * 悬赏消息 WebSocket 会话存储接口
 */
public interface BountyWebSocketSessionStore {

    void add(Long taskId, WebSocketSession session);

    void remove(Long taskId, WebSocketSession session);
}
