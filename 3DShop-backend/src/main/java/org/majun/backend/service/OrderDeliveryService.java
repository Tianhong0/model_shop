package org.majun.backend.service;

import org.majun.backend.dto.DeliveryQueryRequest;
import org.majun.backend.dto.DeliveryShipRequest;
import org.majun.backend.dto.DeliveryStatusUpdateRequest;
import org.majun.backend.dto.DeliveryTrackAddRequest;
import org.majun.backend.dto.DeliveryTrackSimulateRequest;
import org.majun.backend.dto.RetryAutoShipRequest;
import org.majun.backend.vo.DeliveryDetailVO;
import org.majun.backend.vo.DeliveryListVO;
import org.majun.backend.vo.PageResult;

/**
 * 订单物流服务接口
 */
public interface OrderDeliveryService {

    Long shipOrder(DeliveryShipRequest request);

    PageResult<DeliveryListVO> getDeliveryPage(DeliveryQueryRequest request);

    DeliveryDetailVO getAdminDeliveryDetail(Long deliveryId);

    DeliveryDetailVO getUserDeliveryByOrderSn(String orderSn, Long userId);

    void userSignByOrderSn(String orderSn, Long userId);

    void updateDeliveryStatus(DeliveryStatusUpdateRequest request);

    Long addTrack(DeliveryTrackAddRequest request);

    void simulateTracks(DeliveryTrackSimulateRequest request);

    Long autoShipByOrderId(Long orderId, Long printJobId);

    Long retryAutoShip(RetryAutoShipRequest request);

    void autoAdvanceTracks();
}
