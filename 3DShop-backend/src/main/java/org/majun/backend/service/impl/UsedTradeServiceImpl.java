package org.majun.backend.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.CertAlipayRequest;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.ResultCode;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.config.PaymentProperties;
import org.majun.backend.dto.*;
import org.majun.backend.entity.*;
import org.majun.backend.enums.*;
import org.majun.backend.repository.*;
import org.majun.backend.service.UserNotificationService;
import org.majun.backend.service.UsedMessageWebSocketService;
import org.majun.backend.service.UsedTradeService;
import org.majun.backend.vo.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsedTradeServiceImpl implements UsedTradeService {

    private static final DateTimeFormatter ORDER_SN_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter AFTER_SALE_SN_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter ALIPAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String APP_PAY_PRODUCT_CODE = "QUICK_MSECURITY_PAY";
    private static final String APP_PAY_CHANNEL = "ALIPAY_APP";
    private static final String WALLET_PAY_CHANNEL = "WALLET_BALANCE";
    private static final Set<String> ALIPAY_NOTIFY_TRADE_STATUS = Set.of(
            "WAIT_BUYER_PAY",
            "TRADE_SUCCESS",
            "TRADE_FINISHED",
            "TRADE_CLOSED"
    );

    private final UsedListingRepository usedListingRepository;
    private final UsedOfferRepository usedOfferRepository;
    private final UsedOrderRepository usedOrderRepository;
    private final UsedOrderPaymentRepository usedOrderPaymentRepository;
    private final UsedMessageRepository usedMessageRepository;
    private final UsedAfterSaleRepository usedAfterSaleRepository;
    private final UsedReportRepository usedReportRepository;
    private final SysUserRepository sysUserRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final WalletLedgerRepository walletLedgerRepository;
    private final ObjectMapper objectMapper;
    private final UsedMessageWebSocketService usedMessageWebSocketService;
    private final PaymentProperties paymentProperties;
    private final ResourceLoader resourceLoader;
    private final PointService pointService;
    private final UserNotificationService userNotificationService;

    @Override
    public PageResult<UsedListingListVO> pageListings(UsedListingQueryRequest request, Long userId, boolean adminMode) {
        UsedListingQueryRequest safeRequest = request == null ? new UsedListingQueryRequest() : request;
        Page<UsedListing> page = new Page<>(safeRequest.getPageNum(), safeRequest.getPageSize());
        LambdaQueryWrapper<UsedListing> wrapper = new LambdaQueryWrapper<UsedListing>()
                .eq(UsedListing::getIsDelete, 0)
                .orderByDesc(UsedListing::getCreateTime);

        if (safeRequest.getStatus() != null) {
            wrapper.eq(UsedListing::getStatus, safeRequest.getStatus());
        } else if (!adminMode && !Boolean.TRUE.equals(safeRequest.getOnlyMine())) {
            wrapper.eq(UsedListing::getStatus, UsedListingStatus.ON_SALE.getCode());
        }
        if (StringUtils.hasText(safeRequest.getKeyword())) {
            wrapper.and(item -> item.like(UsedListing::getTitle, safeRequest.getKeyword().trim())
                    .or().like(UsedListing::getDescription, safeRequest.getKeyword().trim()));
        }
        if (safeRequest.getSellerId() != null) {
            wrapper.eq(UsedListing::getSellerId, safeRequest.getSellerId());
        }
        if (StringUtils.hasText(safeRequest.getCategoryName())) {
            wrapper.eq(UsedListing::getCategoryName, safeRequest.getCategoryName().trim());
        }
        if (Boolean.TRUE.equals(safeRequest.getOnlyMine())) {
            if (userId == null) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
            }
            wrapper.eq(UsedListing::getSellerId, userId);
        }

        Page<UsedListing> result = usedListingRepository.selectPage(page, wrapper);
        Map<Long, SysUser> userMap = getUserMap(result.getRecords().stream().map(UsedListing::getSellerId).collect(Collectors.toSet()));
        List<UsedListingListVO> records = result.getRecords().stream().map(item -> toListingListVO(item, userMap)).toList();
        return buildPageResult(records, result.getTotal(), safeRequest.getPageNum(), safeRequest.getPageSize(), result.getPages());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UsedListingDetailVO getListingDetail(Long listingId, Long userId, boolean adminMode) {
        UsedListing listing = getListingOrThrow(listingId);
        if (!adminMode && !Objects.equals(listing.getStatus(), UsedListingStatus.ON_SALE.getCode())
                && !Objects.equals(listing.getSellerId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "当前商品不可查看");
        }
        listing.setViewCount((listing.getViewCount() == null ? 0 : listing.getViewCount()) + 1);
        usedListingRepository.updateById(listing);

        Map<Long, SysUser> userMap = getUserMap(Set.of(listing.getSellerId()));
        UsedListingDetailVO vo = new UsedListingDetailVO();
        vo.setId(listing.getId());
        vo.setSellerId(listing.getSellerId());
        SysUser seller = userMap.get(listing.getSellerId());
        vo.setSellerNickname(resolveNickname(seller));
        vo.setSellerAvatar(seller == null ? null : seller.getAvatar());
        vo.setTitle(listing.getTitle());
        vo.setDescription(listing.getDescription());
        vo.setCoverUrl(listing.getCoverUrl());
        vo.setImageUrls(parseImageUrls(listing.getImageUrls()));
        vo.setPrice(listing.getPrice());
        vo.setOriginalPrice(listing.getOriginalPrice());
        vo.setConditionLevel(listing.getConditionLevel());
        vo.setCategoryName(listing.getCategoryName());
        vo.setLocation(listing.getLocation());
        vo.setStatus(listing.getStatus());
        vo.setViewCount(listing.getViewCount());
        vo.setWantCount(listing.getWantCount());
        vo.setCreateTime(listing.getCreateTime());
        boolean owner = userId != null && Objects.equals(listing.getSellerId(), userId);
        vo.setOwner(owner);
        vo.setCanBuy(!owner && Objects.equals(listing.getStatus(), UsedListingStatus.ON_SALE.getCode()));

        List<UsedOffer> offers = usedOfferRepository.selectList(new LambdaQueryWrapper<UsedOffer>()
                .eq(UsedOffer::getListingId, listingId)
                .eq(UsedOffer::getIsDelete, 0)
                .orderByDesc(UsedOffer::getCreateTime));
        if (!owner && userId != null) {
            offers = offers.stream().filter(item -> Objects.equals(item.getBuyerId(), userId)).toList();
        }
        Map<Long, SysUser> offerUserMap = getUserMap(offers.stream().map(UsedOffer::getBuyerId).collect(Collectors.toSet()));
        vo.setOffers(offers.stream().map(item -> toOfferVO(item, offerUserMap)).toList());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createListing(UsedListingCreateRequest request, Long userId) {
        UsedListing listing = new UsedListing();
        fillListing(listing, request, userId);
        listing.setStatus(UsedListingStatus.ON_SALE.getCode());
        listing.setViewCount(0);
        listing.setWantCount(0);
        listing.setIsDelete(0);
        usedListingRepository.insert(listing);
        return listing.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateListing(UsedListingUpdateRequest request, Long userId, boolean adminMode) {
        UsedListing listing = getListingOrThrow(request.getId());
        assertListingEditable(listing, userId, adminMode);
        fillListing(listing, request, listing.getSellerId());
        usedListingRepository.updateById(listing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateListingStatus(UsedListingStatusRequest request, Long userId, boolean adminMode) {
        UsedListing listing = getListingOrThrow(request.getListingId());
        assertListingEditable(listing, userId, adminMode);
        listing.setStatus(request.getStatus());
        usedListingRepository.updateById(listing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOffer(UsedOfferCreateRequest request, Long userId) {
        UsedListing listing = getListingOrThrow(request.getListingId());
        if (!Objects.equals(listing.getStatus(), UsedListingStatus.ON_SALE.getCode())) {
            throw new BusinessException("当前商品不支持议价");
        }
        if (Objects.equals(listing.getSellerId(), userId)) {
            throw new BusinessException("不能给自己的商品议价");
        }
        UsedOffer existed = usedOfferRepository.selectOne(new LambdaQueryWrapper<UsedOffer>()
                .eq(UsedOffer::getListingId, request.getListingId())
                .eq(UsedOffer::getBuyerId, userId)
                .eq(UsedOffer::getIsDelete, 0)
                .orderByDesc(UsedOffer::getCreateTime)
                .last("limit 1"));
        if (existed != null && Objects.equals(existed.getStatus(), UsedOfferStatus.PENDING.getCode())) {
            throw new BusinessException("你有待处理议价，请等待卖家响应");
        }
        UsedOffer offer = new UsedOffer();
        offer.setListingId(request.getListingId());
        offer.setBuyerId(userId);
        offer.setSellerId(listing.getSellerId());
        offer.setOfferAmount(request.getOfferAmount());
        offer.setRemark(trim(request.getRemark()));
        offer.setStatus(UsedOfferStatus.PENDING.getCode());
        offer.setExpireTime(LocalDateTime.now().plusDays(2));
        offer.setIsDelete(0);
        usedOfferRepository.insert(offer);

        listing.setWantCount((listing.getWantCount() == null ? 0 : listing.getWantCount()) + 1);
        usedListingRepository.updateById(listing);
        appendSystemMessage(listing.getId(), listing.getSellerId(), userId, "买家发起了新的议价请求");
        return offer.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void respondOffer(UsedOfferRespondRequest request, Long userId) {
        UsedOffer offer = getOfferOrThrow(request.getOfferId());
        if (!Objects.equals(offer.getSellerId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权处理该议价");
        }
        if (!Objects.equals(offer.getStatus(), UsedOfferStatus.PENDING.getCode())) {
            throw new BusinessException("当前议价已处理");
        }
        offer.setStatus(Boolean.TRUE.equals(request.getApproved()) ? UsedOfferStatus.ACCEPTED.getCode() : UsedOfferStatus.REJECTED.getCode());
        offer.setAcceptedTime(Boolean.TRUE.equals(request.getApproved()) ? LocalDateTime.now() : null);
        offer.setRemark(joinRemark(offer.getRemark(), request.getRemark()));
        usedOfferRepository.updateById(offer);
        appendSystemMessage(offer.getListingId(), offer.getBuyerId(), userId,
                Boolean.TRUE.equals(request.getApproved()) ? "卖家接受了议价，可按议价金额下单" : "卖家拒绝了本次议价");
    }

    @Override
    public PageResult<UsedOrderListVO> pageBuyerOrders(UsedOrderQueryRequest request, Long userId) {
        return pageOrders(request, userId, false, false);
    }

    @Override
    public PageResult<UsedOrderListVO> pageSellerOrders(UsedOrderQueryRequest request, Long userId) {
        return pageOrders(request, userId, true, false);
    }

    @Override
    public PageResult<UsedOrderListVO> pageAdminOrders(UsedOrderQueryRequest request) {
        return pageOrders(request, null, false, true);
    }

    @Override
    public UsedOrderDetailVO getOrderDetail(Long orderId, Long userId, boolean adminMode) {
        UsedOrder order = getOrderOrThrow(orderId);
        if (!adminMode && !Objects.equals(order.getBuyerId(), userId) && !Objects.equals(order.getSellerId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权查看该订单");
        }
        return toOrderDetailVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(UsedOrderCreateRequest request, Long userId) {
        UsedListing listing = getListingOrThrow(request.getListingId());
        if (!Objects.equals(listing.getStatus(), UsedListingStatus.ON_SALE.getCode())) {
            throw new BusinessException("当前商品不可下单");
        }
        if (Objects.equals(listing.getSellerId(), userId)) {
            throw new BusinessException("不能购买自己的商品");
        }
        UsedOrder existedPendingOrder = usedOrderRepository.selectOne(new LambdaQueryWrapper<UsedOrder>()
                .eq(UsedOrder::getListingId, listing.getId())
                .eq(UsedOrder::getBuyerId, userId)
                .eq(UsedOrder::getStatus, UsedOrderStatus.PENDING_PAYMENT.getCode())
                .eq(UsedOrder::getIsDelete, 0)
                .orderByDesc(UsedOrder::getCreateTime)
                .last("limit 1"));
        if (existedPendingOrder != null) {
            throw new BusinessException("你已有该商品的待支付订单，请先完成支付或取消后再重新下单");
        }
        BigDecimal dealAmount = listing.getPrice();
        Long offerId = request.getOfferId();
        if (offerId != null) {
            UsedOffer offer = getOfferOrThrow(offerId);
            if (!Objects.equals(offer.getListingId(), listing.getId()) || !Objects.equals(offer.getBuyerId(), userId)) {
                throw new BusinessException("议价信息不匹配");
            }
            if (!Objects.equals(offer.getStatus(), UsedOfferStatus.ACCEPTED.getCode())) {
                throw new BusinessException("议价尚未被接受");
            }
            dealAmount = offer.getOfferAmount();
        }
        UsedOrder order = new UsedOrder();
        order.setOrderSn(buildUsedOrderSn());
        order.setListingId(listing.getId());
        order.setOfferId(offerId);
        order.setBuyerId(userId);
        order.setSellerId(listing.getSellerId());
        order.setListingTitle(listing.getTitle());
        order.setCoverUrl(listing.getCoverUrl());
        order.setOrderAmount(dealAmount);
        order.setStatus(UsedOrderStatus.PENDING_PAYMENT.getCode());
        order.setReceiverName(trim(request.getReceiverName()));
        order.setReceiverPhone(trim(request.getReceiverPhone()));
        order.setReceiverAddress(trim(request.getReceiverAddress()));
        order.setIsDelete(0);
        usedOrderRepository.insert(order);
        appendSystemMessage(listing.getId(), listing.getSellerId(), userId, "买家已创建订单，等待支付");
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderPayCreateResponse payOrder(Long orderId, Long userId) {
        UsedOrder order = getOrderOrThrow(orderId);
        if (!Objects.equals(order.getBuyerId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权支付该订单");
        }
        if (!Objects.equals(order.getStatus(), UsedOrderStatus.PENDING_PAYMENT.getCode())) {
            throw new BusinessException("当前订单状态不可支付");
        }
        validateAlipayConfig();

        BigDecimal amount = normalizeAmount(order.getOrderAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("订单金额非法，无法发起支付");
        }

        UsedOrderPayment paidRecord = getLatestPaymentByOrderId(order.getId(), PaymentStatus.SUCCESS.getCode());
        if (paidRecord != null) {
            throw new BusinessException("订单已支付，请勿重复支付");
        }

        UsedOrderPayment payment = getLatestPaymentByOrderId(order.getId(), PaymentStatus.PENDING.getCode());
        if (payment == null) {
            payment = new UsedOrderPayment();
            payment.setOrderId(order.getId());
            payment.setOrderSn(order.getOrderSn());
            payment.setOutTradeNo(generateOutTradeNo(order.getId()));
            payment.setTotalAmount(amount);
            payment.setPayChannel(APP_PAY_CHANNEL);
            payment.setPayStatus(PaymentStatus.PENDING.getCode());
            payment.setIsDelete(0);
            usedOrderPaymentRepository.insert(payment);
        } else {
            payment.setTotalAmount(amount);
            payment.setPayChannel(APP_PAY_CHANNEL);
            payment.setNotifyContent(null);
            usedOrderPaymentRepository.updateById(payment);
        }

        String orderString = buildAppPayOrderString(order, payment, amount);
        return new OrderPayCreateResponse(order.getId(), order.getOrderSn(), payment.getOutTradeNo(), amount, orderString);
    }

    @Override
    public OrderPayStatusVO queryPayStatus(Long orderId, Long userId) {
        UsedOrder order = getOrderOrThrow(orderId);
        if (!Objects.equals(order.getBuyerId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权查看该订单支付状态");
        }

        UsedOrderPayment payment = getLatestPayment(order.getId());
        OrderPayStatusVO vo = new OrderPayStatusVO();
        vo.setOrderId(order.getId());
        vo.setOrderSn(order.getOrderSn());
        vo.setOrderStatus(order.getStatus());
        if (payment != null) {
            vo.setPayStatus(payment.getPayStatus());
            vo.setOutTradeNo(payment.getOutTradeNo());
            vo.setTradeNo(payment.getTradeNo());
            vo.setPayTime(payment.getPayTime());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderPayStatusVO payOrderByWallet(Long orderId, Long userId) {
        UsedOrder order = getOrderOrThrow(orderId);
        if (!Objects.equals(order.getBuyerId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权支付该订单");
        }
        if (!Objects.equals(order.getStatus(), UsedOrderStatus.PENDING_PAYMENT.getCode())) {
            throw new BusinessException("当前订单状态不可支付");
        }

        BigDecimal amount = normalizeAmount(order.getOrderAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("订单金额非法，无法发起支付");
        }

        UsedOrderPayment paidRecord = getLatestPaymentByOrderId(order.getId(), PaymentStatus.SUCCESS.getCode());
        if (paidRecord != null) {
            throw new BusinessException("订单已支付，请勿重复支付");
        }

        WalletAccount wallet = getOrCreateWallet(userId);
        debitWalletForUsedOrder(wallet, order, amount);

        UsedOrderPayment payment = new UsedOrderPayment();
        payment.setOrderId(order.getId());
        payment.setOrderSn(order.getOrderSn());
        payment.setOutTradeNo(generateWalletOutTradeNo(order.getId()));
        payment.setTotalAmount(amount);
        payment.setPayChannel(WALLET_PAY_CHANNEL);
        payment.setPayStatus(PaymentStatus.PENDING.getCode());
        payment.setIsDelete(0);
        usedOrderPaymentRepository.insert(payment);

        Map<String, String> walletSnapshot = new HashMap<>();
        walletSnapshot.put("source", "wallet");
        walletSnapshot.put("out_trade_no", payment.getOutTradeNo());
        walletSnapshot.put("trade_status", "TRADE_SUCCESS");
        walletSnapshot.put("total_amount", amount.toPlainString());
        applyTradeStatus(order, payment, "TRADE_SUCCESS", payment.getOutTradeNo(), LocalDateTime.now(), toJson(walletSnapshot));
        return queryPayStatus(orderId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderPayStatusVO syncPayStatus(Long orderId, Long userId) {
        UsedOrder order = getOrderOrThrow(orderId);
        if (!Objects.equals(order.getBuyerId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权同步该订单支付状态");
        }

        UsedOrderPayment payment = getLatestPayment(orderId);
        if (payment == null) {
            throw new BusinessException("支付记录不存在");
        }
        if (Objects.equals(payment.getPayStatus(), PaymentStatus.SUCCESS.getCode())) {
            return queryPayStatus(orderId, userId);
        }

        AlipayTradeQueryResponse queryResponse = queryAlipayTrade(payment.getOutTradeNo());
        if (queryResponse == null) {
            return queryPayStatus(orderId, userId);
        }

        String tradeStatus = queryResponse.getTradeStatus();
        String tradeNo = queryResponse.getTradeNo();
        String totalAmount = queryResponse.getTotalAmount();
        if (StringUtils.hasText(totalAmount)) {
            BigDecimal notifyAmount = normalizeAmount(parseAmount(totalAmount));
            BigDecimal recordAmount = normalizeAmount(payment.getTotalAmount());
            if (notifyAmount.compareTo(recordAmount) != 0) {
                throw new BusinessException("支付金额校验失败");
            }
        }

        Map<String, String> querySnapshot = new HashMap<>();
        querySnapshot.put("source", "query");
        querySnapshot.put("out_trade_no", payment.getOutTradeNo());
        querySnapshot.put("trade_no", tradeNo);
        querySnapshot.put("trade_status", tradeStatus);
        querySnapshot.put("total_amount", totalAmount);
        applyTradeStatus(order, payment, tradeStatus, tradeNo, null, toJson(querySnapshot));
        return queryPayStatus(orderId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleAlipayNotify(Map<String, String> notifyParams) {
        if (notifyParams == null || notifyParams.isEmpty()) {
            throw new BusinessException("支付宝回调参数为空");
        }
        if (!verifyNotifySign(notifyParams)) {
            throw new BusinessException("支付宝回调验签失败");
        }

        String appId = notifyParams.get("app_id");
        if (StringUtils.hasText(paymentProperties.getAppId()) && !Objects.equals(paymentProperties.getAppId(), appId)) {
            throw new BusinessException("回调 appId 不匹配");
        }

        String sellerId = notifyParams.get("seller_id");
        if (StringUtils.hasText(paymentProperties.getSellerId())
                && !Objects.equals(paymentProperties.getSellerId(), sellerId)) {
            throw new BusinessException("回调 sellerId 不匹配");
        }

        String outTradeNo = notifyParams.get("out_trade_no");
        if (!StringUtils.hasText(outTradeNo)) {
            throw new BusinessException("回调缺少 out_trade_no");
        }

        String tradeStatus = notifyParams.get("trade_status");
        if (!StringUtils.hasText(tradeStatus)) {
            throw new BusinessException("回调缺少 trade_status");
        }
        if (!ALIPAY_NOTIFY_TRADE_STATUS.contains(tradeStatus)) {
            throw new BusinessException("回调 trade_status 非法: " + tradeStatus);
        }

        UsedOrderPayment payment = usedOrderPaymentRepository.selectOne(
                new LambdaQueryWrapper<UsedOrderPayment>()
                        .eq(UsedOrderPayment::getOutTradeNo, outTradeNo)
                        .last("limit 1")
        );
        if (payment == null) {
            throw new BusinessException("支付记录不存在: " + outTradeNo);
        }
        if (Objects.equals(payment.getPayStatus(), PaymentStatus.SUCCESS.getCode())) {
            updatePaymentNotifyOnly(payment.getId(), notifyParams);
            return true;
        }

        String tradeNo = notifyParams.get("trade_no");
        if (("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus))
                && !StringUtils.hasText(tradeNo)) {
            throw new BusinessException("回调缺少 trade_no");
        }

        BigDecimal notifyAmount = normalizeAmount(parseAmount(notifyParams.get("total_amount")));
        BigDecimal recordAmount = normalizeAmount(payment.getTotalAmount());
        if (notifyAmount.compareTo(recordAmount) != 0) {
            throw new BusinessException("支付金额校验失败");
        }

        UsedOrder order = getOrderOrThrow(payment.getOrderId());
        LocalDateTime paidTime = parseAlipayTime(notifyParams.get("gmt_payment"));
        applyTradeStatus(order, payment, tradeStatus, tradeNo, paidTime, toJson(notifyParams));
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long userId) {
        UsedOrder order = getOrderOrThrow(orderId);
        if (!Objects.equals(order.getBuyerId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权取消该订单");
        }
        if (!Objects.equals(order.getStatus(), UsedOrderStatus.PENDING_PAYMENT.getCode())) {
            throw new BusinessException("当前订单不可取消");
        }
        order.setStatus(UsedOrderStatus.CANCELED.getCode());
        order.setCancelReason("买家主动取消");
        usedOrderRepository.updateById(order);
        closePendingPayments(order.getId());
        appendSystemMessage(order.getListingId(), order.getSellerId(), userId, "买家取消了订单");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(UsedOrderShipRequest request, Long userId, boolean adminMode) {
        UsedOrder order = getOrderOrThrow(request.getOrderId());
        if (!adminMode && !Objects.equals(order.getSellerId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权发货");
        }
        if (!Objects.equals(order.getStatus(), UsedOrderStatus.WAIT_SHIPMENT.getCode())) {
            throw new BusinessException("当前订单不可发货");
        }
        order.setStatus(UsedOrderStatus.WAIT_RECEIVE.getCode());
        order.setDeliveryCompany(trim(request.getDeliveryCompany()));
        order.setDeliverySn(trim(request.getDeliverySn()));
        order.setDeliveryTime(LocalDateTime.now());
        usedOrderRepository.updateById(order);
        appendSystemMessage(order.getListingId(), order.getBuyerId(), order.getSellerId(), "卖家已发货，可查看物流信息");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(Long orderId, Long userId) {
        UsedOrder order = getOrderOrThrow(orderId);
        if (!Objects.equals(order.getBuyerId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权确认收货");
        }
        if (!Objects.equals(order.getStatus(), UsedOrderStatus.WAIT_RECEIVE.getCode())) {
            throw new BusinessException("当前订单不可确认收货");
        }
        order.setStatus(UsedOrderStatus.COMPLETED.getCode());
        order.setReceiveTime(LocalDateTime.now());
        usedOrderRepository.updateById(order);
        pointService.rewardUsedOrderSell(order.getSellerId(), order.getId(), order.getOrderSn(), order.getOrderAmount());
        appendSystemMessage(order.getListingId(), order.getSellerId(), userId, "买家已确认收货，本次交易已完成");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendMessage(UsedMessageSendRequest request, Long userId) {
        UsedListing listing = getListingOrThrow(request.getListingId());
        validateChatParticipant(listing, userId, request.getCounterpartId());
        UsedMessage message = new UsedMessage();
        message.setListingId(listing.getId());
        message.setRoomKey(buildRoomKey(listing.getId(), userId, request.getCounterpartId()));
        message.setSenderId(userId);
        message.setSenderRole(Objects.equals(userId, listing.getSellerId()) ? "SELLER" : "BUYER");
        message.setCounterpartId(request.getCounterpartId());
        message.setMessageType(request.getMessageType() == null ? 1 : request.getMessageType());
        message.setContent(trim(request.getContent()));
        message.setAttachments(trim(request.getAttachments()));
        message.setIsSystem(0);
        message.setIsDelete(0);
        usedMessageRepository.insert(message);

        Map<Long, SysUser> senderMap = getUserMap(Set.of(userId));
        UsedMessageVO vo = toMessageVO(message, senderMap.get(userId));
        usedMessageWebSocketService.broadcastMessage(message.getRoomKey(), message.getListingId(), vo);
        createUsedChatNotification(listing, message, request.getCounterpartId(), senderMap.get(userId));
        return message.getId();
    }

    @Override
    public List<UsedMessageSessionVO> listMessageSessions(Long listingId, Long userId) {
        UsedListing listing = getListingOrThrow(listingId);
        boolean owner = Objects.equals(listing.getSellerId(), userId);

        List<UsedMessage> rawMessages = usedMessageRepository.selectList(new LambdaQueryWrapper<UsedMessage>()
                .eq(UsedMessage::getListingId, listingId)
                .eq(UsedMessage::getIsDelete, 0)
                .orderByDesc(UsedMessage::getCreateTime));
        if (rawMessages == null || rawMessages.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, UsedMessage> latestMap = new java.util.LinkedHashMap<>();
        for (UsedMessage message : rawMessages) {
            Long counterpartUserId = resolveSessionCounterpartId(message, listing, userId, owner);
            if (counterpartUserId == null || latestMap.containsKey(counterpartUserId)) {
                continue;
            }
            latestMap.put(counterpartUserId, message);
        }
        if (latestMap.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, SysUser> userMap = getUserMap(latestMap.keySet());
        return latestMap.entrySet().stream().map(entry -> {
            Long counterpartUserId = entry.getKey();
            UsedMessage message = entry.getValue();
            SysUser user = userMap.get(counterpartUserId);
            UsedMessageSessionVO vo = new UsedMessageSessionVO();
            vo.setCounterpartId(counterpartUserId);
            vo.setCounterpartNickname(resolveNickname(user));
            vo.setCounterpartAvatar(user == null ? null : user.getAvatar());
            vo.setLastMessage(message.getContent());
            vo.setLastMessageType(message.getMessageType());
            vo.setLastMessageTime(message.getCreateTime());
            return vo;
        }).toList();
    }

    @Override
    public PageResult<UsedMessageVO> pageMessages(Long listingId, Long counterpartId, Integer pageNum, Integer pageSize, Long userId) {
        UsedListing listing = getListingOrThrow(listingId);
        validateChatParticipant(listing, userId, counterpartId);
        String roomKey = buildRoomKey(listingId, userId, counterpartId);
        Page<UsedMessage> page = new Page<>(pageNum, pageSize);
        Page<UsedMessage> result = usedMessageRepository.selectPage(page, new LambdaQueryWrapper<UsedMessage>()
                .eq(UsedMessage::getRoomKey, roomKey)
                .eq(UsedMessage::getIsDelete, 0)
                .orderByDesc(UsedMessage::getCreateTime));
        Map<Long, SysUser> userMap = getUserMap(result.getRecords().stream().map(UsedMessage::getSenderId).collect(Collectors.toSet()));
        List<UsedMessageVO> records = new ArrayList<>(result.getRecords().stream().map(item -> toMessageVO(item, userMap.get(item.getSenderId()))).toList());
        Collections.reverse(records);
        return buildPageResult(records, result.getTotal(), pageNum, pageSize, result.getPages());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAfterSale(UsedAfterSaleCreateRequest request, Long userId) {
        UsedOrder order = getOrderOrThrow(request.getOrderId());
        if (!Objects.equals(order.getBuyerId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权对该订单申请售后");
        }
        if (!Objects.equals(order.getStatus(), UsedOrderStatus.WAIT_RECEIVE.getCode())
                && !Objects.equals(order.getStatus(), UsedOrderStatus.COMPLETED.getCode())) {
            throw new BusinessException("当前订单状态不可申请售后");
        }
        UsedAfterSale existing = usedAfterSaleRepository.selectOne(new LambdaQueryWrapper<UsedAfterSale>()
                .eq(UsedAfterSale::getOrderId, order.getId())
                .eq(UsedAfterSale::getIsDelete, 0)
                .orderByDesc(UsedAfterSale::getCreateTime)
                .last("limit 1"));
        if (existing != null && !Objects.equals(existing.getStatus(), UsedAfterSaleStatus.CLOSED.getCode())
                && !Objects.equals(existing.getStatus(), UsedAfterSaleStatus.REJECTED.getCode())) {
            throw new BusinessException("当前订单已有进行中的售后");
        }
        UsedAfterSale afterSale = new UsedAfterSale();
        afterSale.setAfterSaleSn(buildAfterSaleSn());
        afterSale.setOrderId(order.getId());
        afterSale.setBuyerId(order.getBuyerId());
        afterSale.setSellerId(order.getSellerId());
        afterSale.setType(request.getType());
        afterSale.setReason(trim(request.getReason()));
        afterSale.setDescription(trim(request.getDescription()));
        afterSale.setEvidenceUrls(trim(request.getEvidenceUrls()));
        afterSale.setRequestedAmount(normalizeAmount(request.getRequestedAmount(), order.getOrderAmount()));
        afterSale.setStatus(UsedAfterSaleStatus.APPLIED.getCode());
        afterSale.setIsDelete(0);
        usedAfterSaleRepository.insert(afterSale);

        order.setStatus(UsedOrderStatus.AFTER_SALE.getCode());
        usedOrderRepository.updateById(order);
        appendSystemMessage(order.getListingId(), order.getSellerId(), order.getBuyerId(), "买家发起了售后申请，请及时处理");
        return afterSale.getId();
    }

    @Override
    public PageResult<UsedAfterSaleVO> pageBuyerAfterSales(UsedOrderQueryRequest request, Long userId) {
        return pageAfterSales(request, userId, false);
    }

    @Override
    public PageResult<UsedAfterSaleVO> pageSellerAfterSales(UsedOrderQueryRequest request, Long userId) {
        return pageAfterSales(request, userId, true);
    }

    @Override
    public UsedAfterSaleVO getAfterSaleDetail(Long afterSaleId, Long userId, boolean adminMode) {
        UsedAfterSale afterSale = getAfterSaleOrThrow(afterSaleId);
        if (!adminMode && !Objects.equals(afterSale.getBuyerId(), userId) && !Objects.equals(afterSale.getSellerId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权查看该售后单");
        }
        return toAfterSaleVO(afterSale, getOrderOrThrow(afterSale.getOrderId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditAfterSaleBySeller(UsedAfterSaleAuditRequest request, Long userId) {
        UsedAfterSale afterSale = getAfterSaleOrThrow(request.getAfterSaleId());
        if (!Objects.equals(afterSale.getSellerId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权处理该售后");
        }
        if (!Objects.equals(afterSale.getStatus(), UsedAfterSaleStatus.APPLIED.getCode())
                && !Objects.equals(afterSale.getStatus(), UsedAfterSaleStatus.PLATFORM_INTERVENTION.getCode())) {
            throw new BusinessException("当前售后状态不可处理");
        }
        UsedOrder order = getOrderOrThrow(afterSale.getOrderId());
        if (Boolean.TRUE.equals(request.getApproved())) {
            afterSale.setStatus(UsedAfterSaleStatus.REFUNDED.getCode());
            afterSale.setRefundAmount(normalizeAmount(request.getRefundAmount(), order.getOrderAmount()));
            afterSale.setSellerRemark(trim(request.getRemark()));
            order.setStatus(UsedOrderStatus.CANCELED.getCode());
            order.setCancelReason("售后退款完成");
        } else {
            afterSale.setStatus(UsedAfterSaleStatus.REJECTED.getCode());
            afterSale.setSellerRemark(trim(request.getRemark()));
            order.setStatus(resolveOrderStatusAfterAfterSale(order));
        }
        usedAfterSaleRepository.updateById(afterSale);
        usedOrderRepository.updateById(order);
        appendSystemMessage(order.getListingId(), order.getBuyerId(), userId,
                Boolean.TRUE.equals(request.getApproved()) ? "卖家已同意退款，售后已完成" : "卖家拒绝了售后申请，如有争议可发起举报");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReport(UsedReportCreateRequest request, Long userId) {
        UsedReport report = new UsedReport();
        report.setReporterId(userId);
        report.setTargetType(trim(request.getTargetType()));
        report.setTargetId(request.getTargetId());
        report.setReasonType(trim(request.getReasonType()));
        report.setReasonText(trim(request.getReasonText()));
        report.setEvidenceUrls(trim(request.getEvidenceUrls()));
        report.setStatus(UsedReportStatus.PENDING.getCode());
        report.setIsDelete(0);
        usedReportRepository.insert(report);

        if ("ORDER".equalsIgnoreCase(report.getTargetType())) {
            UsedOrder order = getOrderOrThrow(report.getTargetId());
            UsedAfterSale afterSale = usedAfterSaleRepository.selectOne(new LambdaQueryWrapper<UsedAfterSale>()
                    .eq(UsedAfterSale::getOrderId, order.getId())
                    .eq(UsedAfterSale::getIsDelete, 0)
                    .orderByDesc(UsedAfterSale::getCreateTime)
                    .last("limit 1"));
            if (afterSale != null) {
                afterSale.setStatus(UsedAfterSaleStatus.PLATFORM_INTERVENTION.getCode());
                usedAfterSaleRepository.updateById(afterSale);
            }
        }
        return report.getId();
    }

    @Override
    public PageResult<UsedReportVO> pageAdminReports(UsedReportQueryRequest request) {
        UsedReportQueryRequest safeRequest = request == null ? new UsedReportQueryRequest() : request;
        Page<UsedReport> page = new Page<>(safeRequest.getPageNum(), safeRequest.getPageSize());
        LambdaQueryWrapper<UsedReport> wrapper = new LambdaQueryWrapper<UsedReport>()
                .eq(UsedReport::getIsDelete, 0)
                .orderByDesc(UsedReport::getCreateTime);
        if (safeRequest.getStatus() != null) {
            wrapper.eq(UsedReport::getStatus, safeRequest.getStatus());
        }
        if (StringUtils.hasText(safeRequest.getTargetType())) {
            wrapper.eq(UsedReport::getTargetType, safeRequest.getTargetType().trim());
        }
        Page<UsedReport> result = usedReportRepository.selectPage(page, wrapper);
        Set<Long> userIds = result.getRecords().stream().map(UsedReport::getReporterId).collect(Collectors.toSet());
        userIds.addAll(result.getRecords().stream().map(UsedReport::getHandlerId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, SysUser> userMap = getUserMap(userIds);
        List<UsedReportVO> records = result.getRecords().stream().map(item -> toReportVO(item, userMap)).toList();
        return buildPageResult(records, result.getTotal(), safeRequest.getPageNum(), safeRequest.getPageSize(), result.getPages());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleReport(UsedReportHandleRequest request, Long adminId) {
        UsedReport report = getReportOrThrow(request.getReportId());
        report.setStatus(Boolean.TRUE.equals(request.getApproved()) ? UsedReportStatus.RESOLVED.getCode() : UsedReportStatus.REJECTED.getCode());
        report.setHandleAction(trim(request.getHandleAction()));
        report.setHandleRemark(trim(request.getRemark()));
        report.setHandlerId(adminId);
        usedReportRepository.updateById(report);

        if (Boolean.TRUE.equals(request.getApproved()) && "LISTING".equalsIgnoreCase(report.getTargetType())
                && "OFF_SHELF".equalsIgnoreCase(trim(request.getHandleAction()))) {
            UsedListing listing = getListingOrThrow(report.getTargetId());
            listing.setStatus(UsedListingStatus.OFF_SHELF.getCode());
            usedListingRepository.updateById(listing);
        }
    }

    private void fillListing(UsedListing listing, UsedListingCreateRequest request, Long sellerId) {
        listing.setSellerId(sellerId);
        listing.setTitle(trim(request.getTitle()));
        listing.setDescription(trim(request.getDescription()));
        listing.setCoverUrl(trim(request.getCoverUrl()));
        listing.setImageUrls(writeImageUrls(request.getImageUrls()));
        listing.setPrice(request.getPrice());
        listing.setOriginalPrice(request.getOriginalPrice());
        listing.setConditionLevel(trim(request.getConditionLevel()));
        listing.setCategoryName(trim(request.getCategoryName()));
        listing.setLocation(trim(request.getLocation()));
    }

    private void assertListingEditable(UsedListing listing, Long userId, boolean adminMode) {
        if (!adminMode && !Objects.equals(listing.getSellerId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权操作该商品");
        }
    }

    private PageResult<UsedOrderListVO> pageOrders(UsedOrderQueryRequest request, Long userId, boolean sellerMode, boolean adminMode) {
        UsedOrderQueryRequest safeRequest = request == null ? new UsedOrderQueryRequest() : request;
        Page<UsedOrder> page = new Page<>(safeRequest.getPageNum(), safeRequest.getPageSize());
        LambdaQueryWrapper<UsedOrder> wrapper = new LambdaQueryWrapper<UsedOrder>()
                .eq(UsedOrder::getIsDelete, 0)
                .orderByDesc(UsedOrder::getCreateTime);
        if (safeRequest.getStatus() != null) {
            wrapper.eq(UsedOrder::getStatus, safeRequest.getStatus());
        }
        if (StringUtils.hasText(safeRequest.getKeyword())) {
            wrapper.and(item -> item.like(UsedOrder::getListingTitle, safeRequest.getKeyword().trim())
                    .or().like(UsedOrder::getOrderSn, safeRequest.getKeyword().trim()));
        }
        if (adminMode) {
            if (safeRequest.getSellerId() != null) {
                wrapper.eq(UsedOrder::getSellerId, safeRequest.getSellerId());
            }
            if (safeRequest.getBuyerId() != null) {
                wrapper.eq(UsedOrder::getBuyerId, safeRequest.getBuyerId());
            }
        } else if (sellerMode) {
            wrapper.eq(UsedOrder::getSellerId, userId);
        } else {
            wrapper.eq(UsedOrder::getBuyerId, userId);
        }
        Page<UsedOrder> result = usedOrderRepository.selectPage(page, wrapper);
        Set<Long> userIds = result.getRecords().stream().flatMap(item -> java.util.stream.Stream.of(item.getBuyerId(), item.getSellerId())).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = getUserMap(userIds);
        Map<Long, UsedAfterSale> afterSaleMap = getAfterSaleMap(result.getRecords().stream().map(UsedOrder::getId).collect(Collectors.toSet()));
        List<UsedOrderListVO> records = result.getRecords().stream().map(item -> toOrderListVO(item, userMap, afterSaleMap.get(item.getId()))).toList();
        return buildPageResult(records, result.getTotal(), safeRequest.getPageNum(), safeRequest.getPageSize(), result.getPages());
    }

    private PageResult<UsedAfterSaleVO> pageAfterSales(UsedOrderQueryRequest request, Long userId, boolean sellerMode) {
        UsedOrderQueryRequest safeRequest = request == null ? new UsedOrderQueryRequest() : request;
        Page<UsedAfterSale> page = new Page<>(safeRequest.getPageNum(), safeRequest.getPageSize());
        LambdaQueryWrapper<UsedAfterSale> wrapper = new LambdaQueryWrapper<UsedAfterSale>()
                .eq(UsedAfterSale::getIsDelete, 0)
                .orderByDesc(UsedAfterSale::getCreateTime);
        if (sellerMode) {
            wrapper.eq(UsedAfterSale::getSellerId, userId);
        } else {
            wrapper.eq(UsedAfterSale::getBuyerId, userId);
        }
        Page<UsedAfterSale> result = usedAfterSaleRepository.selectPage(page, wrapper);
        Map<Long, UsedOrder> orderMap = usedOrderRepository.selectBatchIds(result.getRecords().stream().map(UsedAfterSale::getOrderId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(UsedOrder::getId, Function.identity()));
        List<UsedAfterSaleVO> records = result.getRecords().stream().map(item -> toAfterSaleVO(item, orderMap.get(item.getOrderId()))).toList();
        return buildPageResult(records, result.getTotal(), safeRequest.getPageNum(), safeRequest.getPageSize(), result.getPages());
    }

    private UsedOrderDetailVO toOrderDetailVO(UsedOrder order) {
        Map<Long, SysUser> userMap = getUserMap(Set.of(order.getBuyerId(), order.getSellerId()));
        UsedOrderDetailVO vo = new UsedOrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderSn(order.getOrderSn());
        vo.setListingId(order.getListingId());
        vo.setListingTitle(order.getListingTitle());
        vo.setCoverUrl(order.getCoverUrl());
        vo.setBuyerId(order.getBuyerId());
        vo.setSellerId(order.getSellerId());
        vo.setBuyerNickname(resolveNickname(userMap.get(order.getBuyerId())));
        vo.setSellerNickname(resolveNickname(userMap.get(order.getSellerId())));
        vo.setOrderAmount(order.getOrderAmount());
        vo.setStatus(order.getStatus());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setDeliveryCompany(order.getDeliveryCompany());
        vo.setDeliverySn(order.getDeliverySn());
        vo.setDeliveryTime(order.getDeliveryTime());
        vo.setReceiveTime(order.getReceiveTime());
        vo.setCreateTime(order.getCreateTime());
        vo.setPayTime(order.getPayTime());
        vo.setCancelReason(order.getCancelReason());
        UsedAfterSale afterSale = usedAfterSaleRepository.selectOne(new LambdaQueryWrapper<UsedAfterSale>()
                .eq(UsedAfterSale::getOrderId, order.getId())
                .eq(UsedAfterSale::getIsDelete, 0)
                .orderByDesc(UsedAfterSale::getCreateTime)
                .last("limit 1"));
        if (afterSale != null) {
            vo.setAfterSale(toAfterSaleVO(afterSale, order));
        }
        return vo;
    }

    private UsedListingListVO toListingListVO(UsedListing item, Map<Long, SysUser> userMap) {
        UsedListingListVO vo = new UsedListingListVO();
        vo.setId(item.getId());
        vo.setSellerId(item.getSellerId());
        vo.setSellerNickname(resolveNickname(userMap.get(item.getSellerId())));
        vo.setTitle(item.getTitle());
        vo.setCoverUrl(item.getCoverUrl());
        vo.setPrice(item.getPrice());
        vo.setOriginalPrice(item.getOriginalPrice());
        vo.setConditionLevel(item.getConditionLevel());
        vo.setCategoryName(item.getCategoryName());
        vo.setLocation(item.getLocation());
        vo.setStatus(item.getStatus());
        vo.setViewCount(item.getViewCount());
        vo.setWantCount(item.getWantCount());
        vo.setCreateTime(item.getCreateTime());
        return vo;
    }

    private UsedOfferVO toOfferVO(UsedOffer item, Map<Long, SysUser> userMap) {
        UsedOfferVO vo = new UsedOfferVO();
        vo.setId(item.getId());
        vo.setBuyerId(item.getBuyerId());
        vo.setBuyerNickname(resolveNickname(userMap.get(item.getBuyerId())));
        vo.setOfferAmount(item.getOfferAmount());
        vo.setRemark(item.getRemark());
        vo.setStatus(item.getStatus());
        vo.setCreateTime(item.getCreateTime());
        return vo;
    }

    private UsedOrderListVO toOrderListVO(UsedOrder item, Map<Long, SysUser> userMap, UsedAfterSale afterSale) {
        UsedOrderListVO vo = new UsedOrderListVO();
        vo.setId(item.getId());
        vo.setOrderSn(item.getOrderSn());
        vo.setListingId(item.getListingId());
        vo.setListingTitle(item.getListingTitle());
        vo.setCoverUrl(item.getCoverUrl());
        vo.setBuyerId(item.getBuyerId());
        vo.setSellerId(item.getSellerId());
        vo.setBuyerNickname(resolveNickname(userMap.get(item.getBuyerId())));
        vo.setSellerNickname(resolveNickname(userMap.get(item.getSellerId())));
        vo.setOrderAmount(item.getOrderAmount());
        vo.setStatus(item.getStatus());
        vo.setDeliveryCompany(item.getDeliveryCompany());
        vo.setDeliverySn(item.getDeliverySn());
        vo.setCreateTime(item.getCreateTime());
        vo.setPayTime(item.getPayTime());
        if (afterSale != null) {
            vo.setAfterSaleId(afterSale.getId());
            vo.setAfterSaleStatus(afterSale.getStatus());
        }
        return vo;
    }

    private UsedAfterSaleVO toAfterSaleVO(UsedAfterSale item, UsedOrder order) {
        UsedAfterSaleVO vo = new UsedAfterSaleVO();
        vo.setId(item.getId());
        vo.setAfterSaleSn(item.getAfterSaleSn());
        vo.setOrderId(item.getOrderId());
        vo.setOrderSn(order == null ? null : order.getOrderSn());
        vo.setType(item.getType());
        vo.setReason(item.getReason());
        vo.setDescription(item.getDescription());
        vo.setEvidenceUrls(item.getEvidenceUrls());
        vo.setRequestedAmount(item.getRequestedAmount());
        vo.setRefundAmount(item.getRefundAmount());
        vo.setStatus(item.getStatus());
        vo.setSellerRemark(item.getSellerRemark());
        vo.setAdminRemark(item.getAdminRemark());
        vo.setCreateTime(item.getCreateTime());
        vo.setUpdateTime(item.getUpdateTime());
        return vo;
    }

    private UsedMessageVO toMessageVO(UsedMessage item, SysUser sender) {
        UsedMessageVO vo = new UsedMessageVO();
        vo.setId(item.getId());
        vo.setListingId(item.getListingId());
        vo.setSenderId(item.getSenderId());
        vo.setSenderRole(item.getSenderRole());
        vo.setSenderNickname(resolveNickname(sender));
        vo.setCounterpartId(item.getCounterpartId());
        vo.setMessageType(item.getMessageType());
        vo.setContent(item.getContent());
        vo.setAttachments(item.getAttachments());
        vo.setIsSystem(item.getIsSystem());
        vo.setCreateTime(item.getCreateTime());
        return vo;
    }

    private UsedReportVO toReportVO(UsedReport item, Map<Long, SysUser> userMap) {
        UsedReportVO vo = new UsedReportVO();
        vo.setId(item.getId());
        vo.setReporterId(item.getReporterId());
        vo.setReporterNickname(resolveNickname(userMap.get(item.getReporterId())));
        vo.setTargetType(item.getTargetType());
        vo.setTargetId(item.getTargetId());
        vo.setReasonType(item.getReasonType());
        vo.setReasonText(item.getReasonText());
        vo.setEvidenceUrls(item.getEvidenceUrls());
        vo.setStatus(item.getStatus());
        vo.setHandleAction(item.getHandleAction());
        vo.setHandleRemark(item.getHandleRemark());
        vo.setHandlerId(item.getHandlerId());
        vo.setCreateTime(item.getCreateTime());
        vo.setUpdateTime(item.getUpdateTime());
        return vo;
    }

    private <T> PageResult<T> buildPageResult(List<T> records, long total, int pageNum, int pageSize, long pages) {
        return PageResult.<T>builder()
                .records(records)
                .total(total)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .pages((int) pages)
                .build();
    }

    private Map<Long, SysUser> getUserMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        return sysUserRepository.selectBatchIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, UsedAfterSale> getAfterSaleMap(Set<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return new HashMap<>();
        }
        return usedAfterSaleRepository.selectList(new LambdaQueryWrapper<UsedAfterSale>()
                        .in(UsedAfterSale::getOrderId, orderIds)
                        .eq(UsedAfterSale::getIsDelete, 0)
                        .orderByDesc(UsedAfterSale::getCreateTime))
                .stream()
                .collect(Collectors.toMap(UsedAfterSale::getOrderId, Function.identity(), (a, b) -> a));
    }

    private UsedListing getListingOrThrow(Long listingId) {
        UsedListing listing = usedListingRepository.selectById(listingId);
        if (listing == null || Objects.equals(listing.getIsDelete(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "二手商品不存在");
        }
        return listing;
    }

    private UsedOffer getOfferOrThrow(Long offerId) {
        UsedOffer offer = usedOfferRepository.selectById(offerId);
        if (offer == null || Objects.equals(offer.getIsDelete(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "议价记录不存在");
        }
        return offer;
    }

    private UsedOrder getOrderOrThrow(Long orderId) {
        UsedOrder order = usedOrderRepository.selectById(orderId);
        if (order == null || Objects.equals(order.getIsDelete(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "二手订单不存在");
        }
        return order;
    }

    private UsedAfterSale getAfterSaleOrThrow(Long afterSaleId) {
        UsedAfterSale afterSale = usedAfterSaleRepository.selectById(afterSaleId);
        if (afterSale == null || Objects.equals(afterSale.getIsDelete(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "售后单不存在");
        }
        return afterSale;
    }

    private UsedReport getReportOrThrow(Long reportId) {
        UsedReport report = usedReportRepository.selectById(reportId);
        if (report == null || Objects.equals(report.getIsDelete(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "举报记录不存在");
        }
        return report;
    }

    private String writeImageUrls(List<String> imageUrls) {
        try {
            List<String> safeUrls = imageUrls == null ? Collections.emptyList() : imageUrls.stream().filter(StringUtils::hasText).map(String::trim).toList();
            return objectMapper.writeValueAsString(safeUrls);
        } catch (Exception ex) {
            throw new BusinessException("图片数据序列化失败");
        }
    }

    private List<String> parseImageUrls(String imageUrls) {
        if (!StringUtils.hasText(imageUrls)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(imageUrls, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private void validateChatParticipant(UsedListing listing, Long userId, Long counterpartId) {
        if (Objects.equals(userId, counterpartId)) {
            throw new BusinessException("不能给自己发消息");
        }
        if (!Objects.equals(userId, listing.getSellerId()) && !Objects.equals(counterpartId, listing.getSellerId())) {
            throw new BusinessException("会话对象必须包含卖家");
        }
    }

    private Long resolveSessionCounterpartId(UsedMessage message, UsedListing listing, Long userId, boolean owner) {
        if (message == null || listing == null || userId == null) {
            return null;
        }
        if (owner) {
            if (Objects.equals(message.getSenderId(), listing.getSellerId())) {
                return message.getCounterpartId();
            }
            return message.getSenderId();
        }
        if (!Objects.equals(message.getSenderId(), userId) && !Objects.equals(message.getCounterpartId(), userId)) {
            return null;
        }
        return listing.getSellerId();
    }

    private String buildRoomKey(Long listingId, Long userId, Long counterpartId) {
        long min = Math.min(userId, counterpartId);
        long max = Math.max(userId, counterpartId);
        return listingId + ":" + min + ":" + max;
    }

    private void appendSystemMessage(Long listingId, Long counterpartId, Long senderId, String content) {
        if (listingId == null || counterpartId == null || senderId == null || !StringUtils.hasText(content)) {
            return;
        }
        UsedMessage message = new UsedMessage();
        message.setListingId(listingId);
        message.setRoomKey(buildRoomKey(listingId, senderId, counterpartId));
        message.setSenderId(senderId);
        UsedListing listing = getListingOrThrow(listingId);
        message.setSenderRole(Objects.equals(senderId, listing.getSellerId()) ? "SELLER" : "BUYER");
        message.setCounterpartId(counterpartId);
        message.setMessageType(3);
        message.setContent(content.trim());
        message.setIsSystem(1);
        message.setIsDelete(0);
        usedMessageRepository.insert(message);
        Map<Long, SysUser> senderMap = getUserMap(Set.of(senderId));
        UsedMessageVO vo = toMessageVO(message, senderMap.get(senderId));
        usedMessageWebSocketService.broadcastMessage(message.getRoomKey(), listingId, vo);
        createUsedSystemNotification(listing, message, counterpartId, senderMap.get(senderId));
    }

    private void createUsedChatNotification(UsedListing listing, UsedMessage message, Long receiverId, SysUser sender) {
        if (listing == null || message == null || receiverId == null || Objects.equals(message.getSenderId(), receiverId)) {
            return;
        }
        String content = resolveNotificationContent(message);
        UserNotificationCreateCommand command = new UserNotificationCreateCommand();
        command.setUserId(receiverId);
        command.setCategory(UserNotificationServiceImpl.CATEGORY_TRADE);
        command.setNotificationType(UserNotificationServiceImpl.TYPE_USED_CHAT);
        command.setTitle(resolveNickname(sender) + "发来新的二手消息");
        command.setContent(content);
        command.setCoverUrl(listing.getCoverUrl());
        command.setSenderId(message.getSenderId());
        command.setSenderName(resolveNickname(sender));
        command.setBizId(listing.getId());
        command.setBizNo(listing.getTitle());
        command.setRedirectUrl(buildUsedChatRedirectUrl(listing.getId(), message.getSenderId()));
        command.setPopupRequired(true);
        userNotificationService.createNotification(command);
    }

    private void createUsedSystemNotification(UsedListing listing, UsedMessage message, Long receiverId, SysUser sender) {
        if (listing == null || message == null || receiverId == null || Objects.equals(message.getSenderId(), receiverId)) {
            return;
        }
        String notificationType = resolveUsedSystemNotificationType(message.getContent());
        String category = UserNotificationServiceImpl.TYPE_USED_DELIVERY.equals(notificationType)
                ? UserNotificationServiceImpl.CATEGORY_LOGISTICS
                : UserNotificationServiceImpl.CATEGORY_TRADE;
        UserNotificationCreateCommand command = new UserNotificationCreateCommand();
        command.setUserId(receiverId);
        command.setCategory(category);
        command.setNotificationType(notificationType);
        command.setTitle(resolveUsedSystemNotificationTitle(notificationType));
        command.setContent(message.getContent());
        command.setCoverUrl(listing.getCoverUrl());
        command.setSenderId(message.getSenderId());
        command.setSenderName(resolveNickname(sender));
        command.setBizId(listing.getId());
        command.setBizNo(listing.getTitle());
        command.setRedirectUrl(buildUsedChatRedirectUrl(listing.getId(), message.getSenderId()));
        command.setPopupRequired(true);
        userNotificationService.createNotification(command);
    }

    private String resolveNotificationContent(UsedMessage message) {
        if (message == null) {
            return null;
        }
        if (Integer.valueOf(2).equals(message.getMessageType())) {
            return "[图片消息]";
        }
        if (Integer.valueOf(4).equals(message.getMessageType())) {
            return "[视频消息]";
        }
        return trim(message.getContent());
    }

    private String resolveUsedSystemNotificationType(String content) {
        String safeContent = trim(content);
        if (!StringUtils.hasText(safeContent)) {
            return UserNotificationServiceImpl.TYPE_USED_TRADE;
        }
        if (safeContent.contains("议价")) {
            return UserNotificationServiceImpl.TYPE_USED_BARGAIN;
        }
        if (safeContent.contains("发货") || safeContent.contains("物流")) {
            return UserNotificationServiceImpl.TYPE_USED_DELIVERY;
        }
        return UserNotificationServiceImpl.TYPE_USED_TRADE;
    }

    private String resolveUsedSystemNotificationTitle(String notificationType) {
        if (UserNotificationServiceImpl.TYPE_USED_BARGAIN.equals(notificationType)) {
            return "二手议价提醒";
        }
        if (UserNotificationServiceImpl.TYPE_USED_DELIVERY.equals(notificationType)) {
            return "二手发货提醒";
        }
        return "二手交易提醒";
    }

    private String buildUsedChatRedirectUrl(Long listingId, Long counterpartId) {
        return "/pages/used/chat?listingId=" + listingId + "&counterpartId=" + counterpartId;
    }

    private String buildUsedOrderSn() {
        return "U" + LocalDateTime.now().format(ORDER_SN_FORMATTER) + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }

    private String buildAppPayOrderString(UsedOrder order, UsedOrderPayment payment, BigDecimal amount) {
        try {
            AlipayClient alipayClient = buildClient();
            AlipayTradeAppPayRequest alipayRequest = new AlipayTradeAppPayRequest();

            AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
            model.setOutTradeNo(payment.getOutTradeNo());
            model.setProductCode(APP_PAY_PRODUCT_CODE);
            model.setTotalAmount(amount.setScale(2, RoundingMode.HALF_UP).toPlainString());
            model.setSubject("3DShop二手订单-" + order.getOrderSn());
            model.setBody("3DShop 二手交易订单支付");
            model.setTimeoutExpress(resolveTimeoutExpress());
            alipayRequest.setBizModel(model);
            alipayRequest.setNotifyUrl(resolveUsedNotifyUrl());
            if (StringUtils.hasText(paymentProperties.getReturnUrl())) {
                alipayRequest.setReturnUrl(paymentProperties.getReturnUrl());
            }

            AlipayTradeAppPayResponse response = alipayClient.sdkExecute(alipayRequest);
            if (response == null || !response.isSuccess() || !StringUtils.hasText(response.getBody())) {
                String subMsg = response == null ? "null" : response.getSubMsg();
                throw new BusinessException("调用支付宝下单失败: " + subMsg);
            }
            return response.getBody();
        } catch (AlipayApiException e) {
            throw new BusinessException("调用支付宝下单异常", e);
        }
    }

    private AlipayClient buildClient() {
        try {
            CertAlipayRequest certAlipayRequest = new CertAlipayRequest();
            certAlipayRequest.setServerUrl(paymentProperties.getGatewayUrl());
            certAlipayRequest.setAppId(paymentProperties.getAppId());
            certAlipayRequest.setPrivateKey(paymentProperties.getPrivateKey());
            certAlipayRequest.setFormat(paymentProperties.getFormat());
            certAlipayRequest.setCharset(paymentProperties.getCharset());
            certAlipayRequest.setSignType(paymentProperties.getSignType());
            certAlipayRequest.setCertPath(resolveResourcePath(paymentProperties.getAppCertPath()));
            certAlipayRequest.setAlipayPublicCertPath(resolveResourcePath(paymentProperties.getAlipayPublicCertPath()));
            certAlipayRequest.setRootCertPath(resolveResourcePath(paymentProperties.getAlipayRootCertPath()));
            return new DefaultAlipayClient(certAlipayRequest);
        } catch (AlipayApiException e) {
            throw new BusinessException("初始化支付宝证书客户端失败", e);
        }
    }

    private boolean verifyNotifySign(Map<String, String> params) {
        try {
            return AlipaySignature.rsaCertCheckV1(
                    params,
                    resolveResourcePath(paymentProperties.getAlipayPublicCertPath()),
                    paymentProperties.getCharset(),
                    paymentProperties.getSignType()
            );
        } catch (AlipayApiException e) {
            log.error("二手订单支付宝回调验签异常", e);
            return false;
        }
    }

    private AlipayTradeQueryResponse queryAlipayTrade(String outTradeNo) {
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent("{\"out_trade_no\":\"" + outTradeNo + "\"}");
            AlipayTradeQueryResponse response = buildClient().certificateExecute(request);
            if (response == null || !response.isSuccess()) {
                return null;
            }
            return response;
        } catch (Exception ex) {
            log.warn("二手订单主动查单失败 outTradeNo={}", outTradeNo, ex);
            return null;
        }
    }

    private void applyTradeStatus(UsedOrder order,
                                  UsedOrderPayment payment,
                                  String tradeStatus,
                                  String tradeNo,
                                  LocalDateTime paidTime,
                                  String notifyContent) {
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            usedOrderPaymentRepository.update(null,
                    new LambdaUpdateWrapper<UsedOrderPayment>()
                            .eq(UsedOrderPayment::getId, payment.getId())
                            .in(UsedOrderPayment::getPayStatus, PaymentStatus.PENDING.getCode(), PaymentStatus.FAILED.getCode(), PaymentStatus.CLOSED.getCode())
                            .set(UsedOrderPayment::getPayStatus, PaymentStatus.SUCCESS.getCode())
                            .set(UsedOrderPayment::getTradeNo, tradeNo)
                            .set(UsedOrderPayment::getPayTime, paidTime == null ? LocalDateTime.now() : paidTime)
                            .set(UsedOrderPayment::getNotifyContent, notifyContent)
            );

            if (Objects.equals(order.getStatus(), UsedOrderStatus.PENDING_PAYMENT.getCode())) {
                order.setStatus(UsedOrderStatus.WAIT_SHIPMENT.getCode());
                order.setPayTime(paidTime == null ? LocalDateTime.now() : paidTime);
                usedOrderRepository.updateById(order);

                UsedListing listing = getListingOrThrow(order.getListingId());
                listing.setStatus(UsedListingStatus.SOLD.getCode());
                usedListingRepository.updateById(listing);

                List<UsedOffer> offers = usedOfferRepository.selectList(new LambdaQueryWrapper<UsedOffer>()
                        .eq(UsedOffer::getListingId, order.getListingId())
                        .eq(UsedOffer::getIsDelete, 0));
                for (UsedOffer item : offers) {
                    if (Objects.equals(item.getId(), order.getOfferId())) {
                        item.setStatus(UsedOfferStatus.ACCEPTED.getCode());
                    } else if (Objects.equals(item.getStatus(), UsedOfferStatus.PENDING.getCode())) {
                        item.setStatus(UsedOfferStatus.REJECTED.getCode());
                    }
                    usedOfferRepository.updateById(item);
                }
                appendSystemMessage(order.getListingId(), order.getSellerId(), order.getBuyerId(), "买家已完成支付，请尽快发货");
            }
            return;
        }

        if ("TRADE_CLOSED".equals(tradeStatus)) {
            usedOrderPaymentRepository.update(null,
                    new LambdaUpdateWrapper<UsedOrderPayment>()
                            .eq(UsedOrderPayment::getId, payment.getId())
                            .set(UsedOrderPayment::getPayStatus, PaymentStatus.CLOSED.getCode())
                            .set(UsedOrderPayment::getCloseTime, LocalDateTime.now())
                            .set(UsedOrderPayment::getNotifyContent, notifyContent)
            );
            if (Objects.equals(order.getStatus(), UsedOrderStatus.PENDING_PAYMENT.getCode())) {
                order.setStatus(UsedOrderStatus.CANCELED.getCode());
                order.setCancelReason("支付已关闭");
                usedOrderRepository.updateById(order);
            }
            return;
        }

        if (StringUtils.hasText(notifyContent)) {
            usedOrderPaymentRepository.update(null,
                    new LambdaUpdateWrapper<UsedOrderPayment>()
                            .eq(UsedOrderPayment::getId, payment.getId())
                            .set(UsedOrderPayment::getNotifyContent, notifyContent)
            );
            return;
        }

        updatePaymentNotifyOnly(payment.getId(), Collections.singletonMap("tradeStatus", tradeStatus));
    }

    private String resolveUsedNotifyUrl() {
        String notifyUrl = paymentProperties.getNotifyUrl();
        if (!StringUtils.hasText(notifyUrl)) {
            throw new BusinessException("支付宝回调地址未配置");
        }
        if (notifyUrl.contains("/api/orders/pay/alipay/notify")) {
            return notifyUrl.replace("/api/orders/pay/alipay/notify", "/api/used/order/pay/alipay/notify");
        }
        return notifyUrl;
    }

    private String resolveTimeoutExpress() {
        int timeoutMinutes = paymentProperties.getTimeoutMinutes() == null ? 30 : paymentProperties.getTimeoutMinutes();
        return Math.max(timeoutMinutes, 1) + "m";
    }

    private String resolveResourcePath(String location) {
        if (!StringUtils.hasText(location)) {
            return location;
        }
        if (location.startsWith("classpath:")) {
            try {
                Resource resource = resourceLoader.getResource(location);
                String fileName = resource.getFilename();
                String suffix = ".tmp";
                if (StringUtils.hasText(fileName) && fileName.contains(".")) {
                    suffix = fileName.substring(fileName.lastIndexOf('.'));
                }
                Path tempFile = Files.createTempFile("alipay-cert-", suffix);
                try (var inputStream = resource.getInputStream()) {
                    Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }
                tempFile.toFile().deleteOnExit();
                return tempFile.toAbsolutePath().toString();
            } catch (Exception ex) {
                throw new BusinessException("证书文件不存在或不可读: " + location);
            }
        }
        return location;
    }

    private UsedOrderPayment getLatestPayment(Long orderId) {
        return usedOrderPaymentRepository.selectOne(
                new LambdaQueryWrapper<UsedOrderPayment>()
                        .eq(UsedOrderPayment::getOrderId, orderId)
                        .orderByDesc(UsedOrderPayment::getCreateTime)
                        .last("limit 1")
        );
    }

    private UsedOrderPayment getLatestPaymentByOrderId(Long orderId, Integer payStatus) {
        return usedOrderPaymentRepository.selectOne(
                new LambdaQueryWrapper<UsedOrderPayment>()
                        .eq(UsedOrderPayment::getOrderId, orderId)
                        .eq(UsedOrderPayment::getPayStatus, payStatus)
                        .orderByDesc(UsedOrderPayment::getCreateTime)
                        .last("limit 1")
        );
    }

    private void closePendingPayments(Long orderId) {
        usedOrderPaymentRepository.update(null,
                new LambdaUpdateWrapper<UsedOrderPayment>()
                        .eq(UsedOrderPayment::getOrderId, orderId)
                        .eq(UsedOrderPayment::getPayStatus, PaymentStatus.PENDING.getCode())
                        .set(UsedOrderPayment::getPayStatus, PaymentStatus.CLOSED.getCode())
                        .set(UsedOrderPayment::getCloseTime, LocalDateTime.now())
        );
    }

    private void updatePaymentNotifyOnly(Long paymentId, Map<String, String> notifyParams) {
        usedOrderPaymentRepository.update(null,
                new LambdaUpdateWrapper<UsedOrderPayment>()
                        .eq(UsedOrderPayment::getId, paymentId)
                        .set(UsedOrderPayment::getNotifyContent, toJson(notifyParams))
        );
    }

    private String toJson(Map<String, String> data) {
        try {
            Map<String, String> safeData = data == null ? Collections.emptyMap() : data;
            return objectMapper.writeValueAsString(safeData);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String generateOutTradeNo(Long orderId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String orderSuffix = String.valueOf(orderId == null ? 0L : orderId);
        if (orderSuffix.length() > 10) {
            orderSuffix = orderSuffix.substring(orderSuffix.length() - 10);
        }
        return "UP" + timestamp + random + orderSuffix;
    }

    private String generateWalletOutTradeNo(Long orderId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String orderSuffix = String.valueOf(orderId == null ? 0L : orderId);
        if (orderSuffix.length() > 10) {
            orderSuffix = orderSuffix.substring(orderSuffix.length() - 10);
        }
        return "UW" + timestamp + random + orderSuffix;
    }

    private WalletAccount getOrCreateWallet(Long userId) {
        WalletAccount wallet = walletAccountRepository.selectOne(new LambdaQueryWrapper<WalletAccount>()
                .eq(WalletAccount::getUserId, userId)
                .eq(WalletAccount::getIsDelete, 0)
                .last("limit 1"));
        if (wallet != null) {
            return wallet;
        }
        WalletAccount created = new WalletAccount();
        created.setUserId(userId);
        created.setAvailableBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        created.setFrozenBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        created.setStatus(1);
        created.setVersion(0);
        created.setIsDelete(0);
        walletAccountRepository.insert(created);
        return created;
    }

    private void debitWalletForUsedOrder(WalletAccount wallet, UsedOrder order, BigDecimal amount) {
        if (!Objects.equals(wallet.getStatus(), 1)) {
            throw new BusinessException("钱包状态异常，无法支付");
        }
        BigDecimal beforeAvailable = amount(wallet.getAvailableBalance());
        BigDecimal beforeFrozen = amount(wallet.getFrozenBalance());
        if (beforeAvailable.compareTo(amount) < 0) {
            throw new BusinessException("钱包余额不足");
        }

        BigDecimal afterAvailable = beforeAvailable.subtract(amount);
        int updated = walletAccountRepository.update(null,
                new LambdaUpdateWrapper<WalletAccount>()
                        .eq(WalletAccount::getId, wallet.getId())
                        .eq(WalletAccount::getVersion, wallet.getVersion())
                        .eq(WalletAccount::getIsDelete, 0)
                        .set(WalletAccount::getAvailableBalance, afterAvailable)
                        .set(WalletAccount::getVersion, wallet.getVersion() + 1)
        );
        if (updated <= 0) {
            throw new BusinessException("钱包余额变更失败，请重试");
        }

        WalletLedger ledger = new WalletLedger();
        ledger.setUserId(order.getBuyerId());
        ledger.setAccountId(wallet.getId());
        ledger.setDirection(WalletLedgerDirection.EXPENSE.getCode());
        ledger.setBizType("USED_ORDER_PAY_BALANCE");
        ledger.setBizNo(String.valueOf(order.getId()));
        ledger.setRefId(order.getId());
        ledger.setAmount(amount);
        ledger.setBeforeAvailable(beforeAvailable);
        ledger.setAfterAvailable(afterAvailable);
        ledger.setBeforeFrozen(beforeFrozen);
        ledger.setAfterFrozen(beforeFrozen);
        ledger.setRemark("二手订单余额支付");
        walletLedgerRepository.insert(ledger);

        wallet.setAvailableBalance(afterAvailable);
        wallet.setVersion(wallet.getVersion() + 1);
    }

    private BigDecimal amount(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal parseAmount(String totalAmount) {
        if (!StringUtils.hasText(totalAmount)) {
            throw new BusinessException("回调缺少 total_amount");
        }
        try {
            return new BigDecimal(totalAmount.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("回调金额格式非法");
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        return safeAmount.setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDateTime parseAlipayTime(String gmtPayment) {
        if (!StringUtils.hasText(gmtPayment)) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(gmtPayment, ALIPAY_TIME_FORMATTER);
        } catch (Exception ex) {
            return LocalDateTime.now();
        }
    }

    private void validateAlipayConfig() {
        if (!StringUtils.hasText(paymentProperties.getAppId())
                || !StringUtils.hasText(paymentProperties.getPrivateKey())
                || !StringUtils.hasText(paymentProperties.getAppCertPath())
                || !StringUtils.hasText(paymentProperties.getAlipayPublicCertPath())
                || !StringUtils.hasText(paymentProperties.getAlipayRootCertPath())
                || !StringUtils.hasText(paymentProperties.getGatewayUrl())
                || !StringUtils.hasText(paymentProperties.getNotifyUrl())) {
            throw new BusinessException("支付宝证书模式配置不完整，请检查 payment.alipay 配置");
        }
    }

    private String buildAfterSaleSn() {
        return "USA" + LocalDateTime.now().format(AFTER_SALE_SN_FORMATTER) + UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
    }

    private String resolveNickname(SysUser user) {
        if (user == null) {
            return "用户";
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        return StringUtils.hasText(user.getUserName()) ? user.getUserName() : "用户";
    }

    private String trim(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private String joinRemark(String origin, String append) {
        String appendText = trim(append);
        if (!StringUtils.hasText(appendText)) {
            return origin;
        }
        if (!StringUtils.hasText(origin)) {
            return appendText;
        }
        return origin + "\n" + appendText;
    }

    private BigDecimal normalizeAmount(BigDecimal amount, BigDecimal defaultValue) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return defaultValue;
        }
        return amount;
    }

    private Integer resolveOrderStatusAfterAfterSale(UsedOrder order) {
        if (order.getReceiveTime() != null) {
            return UsedOrderStatus.COMPLETED.getCode();
        }
        return UsedOrderStatus.WAIT_RECEIVE.getCode();
    }
}
