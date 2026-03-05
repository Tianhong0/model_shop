package org.majun.backend.service;

import org.majun.backend.vo.BountyPayCreateResponse;
import org.majun.backend.vo.BountyPayStatusVO;

import java.util.Map;

public interface BountyPaymentService {

    BountyPayCreateResponse createPriceIncreasePayOrder(Long priceChangeId, Long userId);

    BountyPayStatusVO queryPriceIncreasePayStatus(Long priceChangeId, Long userId);

    BountyPayStatusVO syncPriceIncreasePayStatus(Long priceChangeId, Long userId);

    BountyPayStatusVO queryPriceIncreasePayStatusByTask(Long taskId, Long userId, boolean adminMode);

    BountyPayStatusVO syncPriceIncreasePayStatusByTask(Long taskId, Long userId, boolean adminMode);

    boolean handleAlipayNotify(Map<String, String> notifyParams);
}
