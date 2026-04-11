package org.majun.backend.service;

import org.majun.backend.dto.OrderPayCreateRequest;
import org.majun.backend.dto.OrderBatchPayCreateRequest;
import org.majun.backend.vo.OrderBatchPayCreateResponse;
import org.majun.backend.vo.OrderBatchPayStatusVO;
import org.majun.backend.vo.OrderPayCreateResponse;
import org.majun.backend.vo.OrderPayStatusVO;

import java.util.Map;

/**
 * 订单支付服务接口
 */
public interface OrderPaymentService {

    OrderPayCreateResponse createAppPayOrder(OrderPayCreateRequest request, Long userId);

    OrderBatchPayCreateResponse createBatchAppPayOrder(OrderBatchPayCreateRequest request, Long userId);

    OrderPayStatusVO payOrderByWallet(OrderPayCreateRequest request, Long userId);

    OrderBatchPayStatusVO payBatchByWallet(OrderBatchPayCreateRequest request, Long userId);

    OrderPayStatusVO queryPayStatus(Long orderId, Long userId);

    OrderPayStatusVO syncPayStatus(Long orderId, Long userId);

    OrderBatchPayStatusVO queryBatchPayStatus(Long batchId, Long userId);

    OrderBatchPayStatusVO syncBatchPayStatus(Long batchId, Long userId);

    boolean handleAlipayNotify(Map<String, String> notifyParams);

    void closeTimeoutPayments();
}
