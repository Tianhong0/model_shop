package org.majun.backend.service;

import org.majun.backend.vo.PrintJobProgressVO;

/**
 * 打印进度 WebSocket 服务接口
 */
public interface PrintWebSocketService {

    void broadcast(PrintJobProgressVO payload);
}
