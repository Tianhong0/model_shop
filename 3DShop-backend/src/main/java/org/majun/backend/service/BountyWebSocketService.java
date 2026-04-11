package org.majun.backend.service;

import org.majun.backend.vo.BountyMessageVO;

/**
 * 悬赏消息 WebSocket 服务接口
 */
public interface BountyWebSocketService {

    void broadcastMessage(Long taskId, BountyMessageVO message);
}
