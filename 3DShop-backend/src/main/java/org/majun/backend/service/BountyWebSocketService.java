package org.majun.backend.service;

import org.majun.backend.vo.BountyMessageVO;

public interface BountyWebSocketService {

    void broadcastMessage(Long taskId, BountyMessageVO message);
}
