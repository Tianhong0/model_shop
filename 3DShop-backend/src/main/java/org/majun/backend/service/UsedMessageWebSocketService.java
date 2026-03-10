package org.majun.backend.service;

import org.majun.backend.vo.UsedMessageVO;

public interface UsedMessageWebSocketService {

    void broadcastMessage(String roomKey, Long listingId, UsedMessageVO message);
}
