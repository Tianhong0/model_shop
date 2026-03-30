package org.majun.backend.service;

import org.majun.backend.vo.BountyPayCreateResponse;
import org.majun.backend.vo.BountyPayStatusVO;

import java.util.Map;

public interface BountyPaymentService {

    // ==================== 托管金支付 ====================

    BountyPayCreateResponse createEscrowPayOrder(Long taskId, Long userId);

    BountyPayStatusVO queryEscrowPayStatus(Long taskId, Long userId);

    BountyPayStatusVO syncEscrowPayStatus(Long taskId, Long userId);

    // ==================== 改价补差支付 ====================

    BountyPayCreateResponse createPriceIncreasePayOrder(Long priceChangeId, Long userId);

    BountyPayStatusVO queryPriceIncreasePayStatus(Long priceChangeId, Long userId);

    BountyPayStatusVO syncPriceIncreasePayStatus(Long priceChangeId, Long userId);

    BountyPayStatusVO queryPriceIncreasePayStatusByTask(Long taskId, Long userId, boolean adminMode);

    BountyPayStatusVO syncPriceIncreasePayStatusByTask(Long taskId, Long userId, boolean adminMode);

    // ==================== 支付回调 ====================

    boolean handleAlipayNotify(Map<String, String> notifyParams);
}
