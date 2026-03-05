package org.majun.backend.service;

import org.majun.backend.vo.PrintJobProgressVO;

public interface PrintWebSocketService {

    void broadcast(PrintJobProgressVO payload);
}
