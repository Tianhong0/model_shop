package org.majun.backend.service;

import org.majun.backend.dto.*;
import org.majun.backend.vo.*;

import java.util.Map;

public interface UsedTradeService {

    PageResult<UsedListingListVO> pageListings(UsedListingQueryRequest request, Long userId, boolean adminMode);

    UsedListingDetailVO getListingDetail(Long listingId, Long userId, boolean adminMode);

    Long createListing(UsedListingCreateRequest request, Long userId);

    void updateListing(UsedListingUpdateRequest request, Long userId, boolean adminMode);

    void updateListingStatus(UsedListingStatusRequest request, Long userId, boolean adminMode);

    Long createOffer(UsedOfferCreateRequest request, Long userId);

    void respondOffer(UsedOfferRespondRequest request, Long userId);

    PageResult<UsedOrderListVO> pageBuyerOrders(UsedOrderQueryRequest request, Long userId);

    PageResult<UsedOrderListVO> pageSellerOrders(UsedOrderQueryRequest request, Long userId);

    PageResult<UsedOrderListVO> pageAdminOrders(UsedOrderQueryRequest request);

    UsedOrderDetailVO getOrderDetail(Long orderId, Long userId, boolean adminMode);

    Long createOrder(UsedOrderCreateRequest request, Long userId);

    OrderPayCreateResponse payOrder(Long orderId, Long userId);

    OrderPayStatusVO payOrderByWallet(Long orderId, Long userId);

    OrderPayStatusVO queryPayStatus(Long orderId, Long userId);

    OrderPayStatusVO syncPayStatus(Long orderId, Long userId);

    boolean handleAlipayNotify(Map<String, String> notifyParams);

    void cancelOrder(Long orderId, Long userId);

    void shipOrder(UsedOrderShipRequest request, Long userId, boolean adminMode);

    void confirmReceive(Long orderId, Long userId);

    Long sendMessage(UsedMessageSendRequest request, Long userId);

    java.util.List<UsedMessageSessionVO> listMessageSessions(Long listingId, Long userId);

    PageResult<UsedMessageVO> pageMessages(Long listingId, Long counterpartId, Integer pageNum, Integer pageSize, Long userId);

    Long createAfterSale(UsedAfterSaleCreateRequest request, Long userId);

    PageResult<UsedAfterSaleVO> pageBuyerAfterSales(UsedOrderQueryRequest request, Long userId);

    PageResult<UsedAfterSaleVO> pageSellerAfterSales(UsedOrderQueryRequest request, Long userId);

    UsedAfterSaleVO getAfterSaleDetail(Long afterSaleId, Long userId, boolean adminMode);

    void auditAfterSaleBySeller(UsedAfterSaleAuditRequest request, Long userId);

    Long createReport(UsedReportCreateRequest request, Long userId);

    PageResult<UsedReportVO> pageAdminReports(UsedReportQueryRequest request);

    void handleReport(UsedReportHandleRequest request, Long adminId);
}
