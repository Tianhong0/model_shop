package org.majun.backend.service;

import org.majun.backend.dto.OrderCreateRequest;
import org.majun.backend.dto.OrderQueryRequest;
import org.majun.backend.dto.OrderStatusUpdateRequest;
import org.majun.backend.vo.OrderCreateResponse;
import org.majun.backend.vo.OrderDetailVO;
import org.majun.backend.vo.OrderListVO;
import org.majun.backend.vo.PageResult;

/**
 * Order service.
 */
public interface OrderService {

    OrderCreateResponse createOrder(OrderCreateRequest request, Long userId);

    PageResult<OrderListVO> getUserOrders(OrderQueryRequest request, Long userId);

    PageResult<OrderListVO> getAdminOrders(OrderQueryRequest request);

    OrderDetailVO getOrderDetail(Long orderId, Long userId);

    OrderDetailVO getOrderDetailByOrderSn(String orderSn, Long userId);

    OrderDetailVO getAdminOrderDetail(Long orderId);

    void cancelOrder(Long orderId, Long userId);

    void deleteOrder(Long orderId, Long userId);

    void updateOrderStatus(OrderStatusUpdateRequest request);
}
