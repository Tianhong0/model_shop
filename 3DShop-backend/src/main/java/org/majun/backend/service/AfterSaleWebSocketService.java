package org.majun.backend.service;

import org.majun.backend.vo.AfterSaleMessageVO;

public interface AfterSaleWebSocketService {

    void broadcastMessage(Long afterSaleId, AfterSaleMessageVO message);
}
