package org.majun.backend.service;

import org.majun.backend.vo.UsedMessageVO;

/**
 * 二手消息 WebSocket 服务接口
 */
public interface UsedMessageWebSocketService {

    void broadcastMessage(String roomKey, Long listingId, UsedMessageVO message);
}
