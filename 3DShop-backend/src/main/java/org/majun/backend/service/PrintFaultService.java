package org.majun.backend.service;

import org.majun.backend.vo.PrintFaultDiagnosisVO;

/**
 * 打印故障诊断服务接口
 */
public interface PrintFaultService {

    /**
     * 根据订单ID诊断故障
     */
    PrintFaultDiagnosisVO diagnoseByOrderId(Long orderId, Long userId);

    /**
     * 用户重试打印
     */
    void userRetryPrint(Long orderId, Long userId);
}
