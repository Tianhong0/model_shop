package org.majun.backend.service;

import org.majun.backend.vo.AfterSaleMessageVO;

/**
 * 售后消息 WebSocket 服务接口
 */
public interface AfterSaleWebSocketService {

    void broadcastMessage(Long afterSaleId, AfterSaleMessageVO message);
}
