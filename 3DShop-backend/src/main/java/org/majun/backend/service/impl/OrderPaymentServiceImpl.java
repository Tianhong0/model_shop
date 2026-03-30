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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.config.PaymentProperties;
import org.majun.backend.dto.OrderBatchPayCreateRequest;
import org.majun.backend.dto.OrderPayCreateRequest;
import org.majun.backend.event.OrderPaidEvent;
import org.majun.backend.entity.SysOrderPayBatch;
import org.majun.backend.entity.SysOrderPayBatchItem;
import org.majun.backend.entity.SysOrder;
import org.majun.backend.entity.SysOrderPayment;
import org.majun.backend.entity.WalletAccount;
import org.majun.backend.entity.WalletLedger;
import org.majun.backend.entity.ModelMaterial;
import org.majun.backend.enums.OrderStatus;
import org.majun.backend.enums.PaymentStatus;
import org.majun.backend.enums.WalletLedgerDirection;
import org.majun.backend.repository.SysOrderPayBatchItemRepository;
import org.majun.backend.repository.SysOrderPayBatchRepository;
import org.majun.backend.repository.SysOrderPaymentRepository;
import org.majun.backend.repository.SysOrderRepository;
import org.majun.backend.repository.WalletAccountRepository;
import org.majun.backend.repository.WalletLedgerRepository;
import org.majun.backend.repository.ModelMaterialRepository;
import org.majun.backend.service.OrderPaymentService;
import org.majun.backend.vo.OrderBatchPayCreateResponse;
import org.majun.backend.vo.OrderBatchPayStatusVO;
import org.majun.backend.vo.OrderPayCreateResponse;
import org.majun.backend.vo.OrderPayStatusVO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderPaymentServiceImpl implements OrderPaymentService {

    private static final String APP_PAY_PRODUCT_CODE = "QUICK_MSECURITY_PAY";
    private static final String APP_PAY_CHANNEL = "ALIPAY_APP";
    private static final String WALLET_PAY_CHANNEL = "WALLET_BALANCE";
    private static final DateTimeFormatter ALIPAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        private static final Set<String> ALIPAY_NOTIFY_TRADE_STATUS = Set.of(
            "WAIT_BUYER_PAY",
            "TRADE_SUCCESS",
            "TRADE_FINISHED",
            "TRADE_CLOSED"
        );

    private final PaymentProperties paymentProperties;
    private final SysOrderRepository orderRepository;
    private final SysOrderPaymentRepository orderPaymentRepository;
    private final SysOrderPayBatchRepository orderPayBatchRepository;
    private final SysOrderPayBatchItemRepository orderPayBatchItemRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final WalletLedgerRepository walletLedgerRepository;
    private final ModelMaterialRepository modelMaterialRepository;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final PointService pointService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderPayCreateResponse createAppPayOrder(OrderPayCreateRequest request, Long userId) {
        validateAlipayConfig();

        SysOrder order = getOrderOrThrow(request.getOrderId());
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException("无权发起该订单支付");
        }
        if (!Objects.equals(order.getOrderStatus(), OrderStatus.PENDING_PAYMENT.getCode())) {
            throw new BusinessException("当前订单状态不支持发起支付");
        }

        BigDecimal amount = normalizeAmount(order.getOrderPrice());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("订单金额非法，无法发起支付");
        }

        SysOrderPayment paidRecord = getLatestPaymentByOrderId(order.getId(), PaymentStatus.SUCCESS.getCode());
        if (paidRecord != null) {
            throw new BusinessException("订单已支付，请勿重复支付");
        }

        SysOrderPayment payment = getLatestPaymentByOrderId(order.getId(), PaymentStatus.PENDING.getCode());
        if (payment == null) {
            payment = new SysOrderPayment();
            payment.setOrderId(order.getId());
            payment.setOrderSn(order.getOrderSn());
            payment.setOutTradeNo(generateOutTradeNo(order.getId()));
            payment.setTotalAmount(amount);
            payment.setPayChannel(APP_PAY_CHANNEL);
            payment.setPayStatus(PaymentStatus.PENDING.getCode());
            orderPaymentRepository.insert(payment);
        } else {
            payment.setTotalAmount(amount);
            payment.setPayChannel(APP_PAY_CHANNEL);
            payment.setNotifyContent(null);
            orderPaymentRepository.updateById(payment);
        }

        String orderString = buildAppPayOrderString(order, payment, amount);
        return new OrderPayCreateResponse(order.getId(), order.getOrderSn(), payment.getOutTradeNo(), amount, orderString);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderBatchPayCreateResponse createBatchAppPayOrder(OrderBatchPayCreateRequest request, Long userId) {
        validateAlipayConfig();

        List<Long> rawOrderIds = request == null ? Collections.emptyList() : request.getOrderIds();
        if (rawOrderIds == null || rawOrderIds.isEmpty()) {
            throw new BusinessException("订单列表不能为空");
        }

        List<Long> orderIds = new ArrayList<>(new LinkedHashSet<>(rawOrderIds));
        if (orderIds.isEmpty()) {
            throw new BusinessException("订单列表不能为空");
        }
        List<SysOrder> orders = orderRepository.selectBatchIds(orderIds);
        if (orders == null || orders.size() != orderIds.size()) {
            throw new BusinessException("存在无效订单，无法发起合并支付");
        }

        Map<Long, SysOrder> orderMap = orders.stream().collect(Collectors.toMap(SysOrder::getId, item -> item));
        List<SysOrder> orderedList = orderIds.stream().map(orderMap::get).collect(Collectors.toList());

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (SysOrder order : orderedList) {
            if (!Objects.equals(order.getUserId(), userId)) {
                throw new BusinessException("无权发起该订单支付");
            }
            if (!Objects.equals(order.getOrderStatus(), OrderStatus.PENDING_PAYMENT.getCode())) {
                throw new BusinessException("存在非待支付订单，无法合并支付");
            }
            SysOrderPayment paidRecord = getLatestPaymentByOrderId(order.getId(), PaymentStatus.SUCCESS.getCode());
            if (paidRecord != null) {
                throw new BusinessException("存在已支付订单，请刷新后重试");
            }

            BigDecimal amount = normalizeAmount(order.getOrderPrice());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("订单金额非法，无法发起支付");
            }
            totalAmount = totalAmount.add(amount);
        }

        SysOrderPayBatch batch = new SysOrderPayBatch();
        batch.setUserId(userId);
        batch.setOutTradeNo(generateBatchOutTradeNo(userId));
        batch.setTotalAmount(normalizeAmount(totalAmount));
        batch.setPayChannel(APP_PAY_CHANNEL);
        batch.setPayStatus(PaymentStatus.PENDING.getCode());
        orderPayBatchRepository.insert(batch);

        for (SysOrder order : orderedList) {
            SysOrderPayBatchItem item = new SysOrderPayBatchItem();
            item.setBatchId(batch.getId());
            item.setOrderId(order.getId());
            item.setOrderSn(order.getOrderSn());
            item.setOrderAmount(normalizeAmount(order.getOrderPrice()));
            orderPayBatchItemRepository.insert(item);
        }

        String orderString = buildBatchAppPayOrderString(batch, orderedList.size());
        return new OrderBatchPayCreateResponse(batch.getId(), batch.getOutTradeNo(), batch.getTotalAmount(), orderIds, orderString);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderPayStatusVO payOrderByWallet(OrderPayCreateRequest request, Long userId) {
        SysOrder order = getOrderOrThrow(request.getOrderId());
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException("无权发起该订单支付");
        }
        if (!Objects.equals(order.getOrderStatus(), OrderStatus.PENDING_PAYMENT.getCode())) {
            throw new BusinessException("当前订单状态不支持发起支付");
        }

        BigDecimal amount = normalizeAmount(order.getOrderPrice());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("订单金额非法，无法发起支付");
        }

        SysOrderPayment paidRecord = getLatestPaymentByOrderId(order.getId(), PaymentStatus.SUCCESS.getCode());
        if (paidRecord != null) {
            throw new BusinessException("订单已支付，请勿重复支付");
        }

        WalletAccount wallet = getOrCreateWallet(userId);
        debitWalletForOrder(wallet, order, amount);

        SysOrderPayment payment = new SysOrderPayment();
        payment.setOrderId(order.getId());
        payment.setOrderSn(order.getOrderSn());
        payment.setOutTradeNo(generateWalletOutTradeNo(order.getId()));
        payment.setTotalAmount(amount);
        payment.setPayChannel(WALLET_PAY_CHANNEL);
        payment.setPayStatus(PaymentStatus.PENDING.getCode());
        orderPaymentRepository.insert(payment);

        Map<String, String> walletSnapshot = new HashMap<>();
        walletSnapshot.put("source", "wallet");
        walletSnapshot.put("out_trade_no", payment.getOutTradeNo());
        walletSnapshot.put("trade_status", "TRADE_SUCCESS");
        walletSnapshot.put("total_amount", amount.toPlainString());
        applyTradeStatus(order, payment, "TRADE_SUCCESS", payment.getOutTradeNo(), LocalDateTime.now(), toJson(walletSnapshot));
        return queryPayStatus(order.getId(), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderBatchPayStatusVO payBatchByWallet(OrderBatchPayCreateRequest request, Long userId) {
        List<Long> rawOrderIds = request == null ? Collections.emptyList() : request.getOrderIds();
        if (rawOrderIds == null || rawOrderIds.isEmpty()) {
            throw new BusinessException("订单列表不能为空");
        }

        List<Long> orderIds = new ArrayList<>(new LinkedHashSet<>(rawOrderIds));
        if (orderIds.isEmpty()) {
            throw new BusinessException("订单列表不能为空");
        }
        List<SysOrder> orders = orderRepository.selectBatchIds(orderIds);
        if (orders == null || orders.size() != orderIds.size()) {
            throw new BusinessException("存在无效订单，无法发起余额支付");
        }

        Map<Long, SysOrder> orderMap = orders.stream().collect(Collectors.toMap(SysOrder::getId, item -> item));
        List<SysOrder> orderedList = orderIds.stream().map(orderMap::get).collect(Collectors.toList());

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (SysOrder order : orderedList) {
            if (!Objects.equals(order.getUserId(), userId)) {
                throw new BusinessException("无权发起该订单支付");
            }
            if (!Objects.equals(order.getOrderStatus(), OrderStatus.PENDING_PAYMENT.getCode())) {
                throw new BusinessException("存在非待支付订单，无法余额支付");
            }
            SysOrderPayment paidRecord = getLatestPaymentByOrderId(order.getId(), PaymentStatus.SUCCESS.getCode());
            if (paidRecord != null) {
                throw new BusinessException("存在已支付订单，请刷新后重试");
            }
            BigDecimal amount = normalizeAmount(order.getOrderPrice());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("订单金额非法，无法发起支付");
            }
            totalAmount = totalAmount.add(amount);
        }

        WalletAccount wallet = getOrCreateWallet(userId);
        BigDecimal normalizedTotal = normalizeAmount(totalAmount);
        if (amount(wallet.getAvailableBalance()).compareTo(normalizedTotal) < 0) {
            throw new BusinessException("钱包余额不足");
        }

        SysOrderPayBatch batch = new SysOrderPayBatch();
        batch.setUserId(userId);
        batch.setOutTradeNo(generateWalletBatchOutTradeNo(userId));
        batch.setTotalAmount(normalizedTotal);
        batch.setPayChannel(WALLET_PAY_CHANNEL);
        batch.setPayStatus(PaymentStatus.PENDING.getCode());
        orderPayBatchRepository.insert(batch);

        for (SysOrder order : orderedList) {
            BigDecimal orderAmount = normalizeAmount(order.getOrderPrice());
            debitWalletForOrder(wallet, order, orderAmount);

            SysOrderPayBatchItem item = new SysOrderPayBatchItem();
            item.setBatchId(batch.getId());
            item.setOrderId(order.getId());
            item.setOrderSn(order.getOrderSn());
            item.setOrderAmount(orderAmount);
            orderPayBatchItemRepository.insert(item);
        }

        Map<String, String> walletSnapshot = new HashMap<>();
        walletSnapshot.put("source", "wallet");
        walletSnapshot.put("out_trade_no", batch.getOutTradeNo());
        walletSnapshot.put("trade_status", "TRADE_SUCCESS");
        walletSnapshot.put("total_amount", normalizedTotal.toPlainString());
        applyBatchTradeStatus(batch, "TRADE_SUCCESS", batch.getOutTradeNo(), LocalDateTime.now(), toJson(walletSnapshot));
        return queryBatchPayStatus(batch.getId(), userId);
    }

    @Override
    public OrderPayStatusVO queryPayStatus(Long orderId, Long userId) {
        SysOrder order = getOrderOrThrow(orderId);
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException("无权查看该订单支付状态");
        }

        SysOrderPayment payment = getLatestPayment(order.getId());
        OrderPayStatusVO vo = new OrderPayStatusVO();
        vo.setOrderId(order.getId());
        vo.setOrderSn(order.getOrderSn());
        vo.setOrderStatus(order.getOrderStatus());
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
    public OrderPayStatusVO syncPayStatus(Long orderId, Long userId) {
        SysOrder order = getOrderOrThrow(orderId);
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException("无权同步该订单支付状态");
        }

        SysOrderPayment payment = getLatestPayment(orderId);
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
    public OrderBatchPayStatusVO queryBatchPayStatus(Long batchId, Long userId) {
        SysOrderPayBatch batch = getBatchOrThrow(batchId);
        if (!Objects.equals(batch.getUserId(), userId)) {
            throw new BusinessException("无权查看该支付状态");
        }

        List<SysOrderPayBatchItem> items = getBatchItems(batch.getId());
        Map<Long, SysOrder> orderMap = loadOrderMap(items.stream().map(SysOrderPayBatchItem::getOrderId).collect(Collectors.toList()));

        OrderBatchPayStatusVO vo = new OrderBatchPayStatusVO();
        vo.setBatchId(batch.getId());
        vo.setPayStatus(batch.getPayStatus());
        vo.setBatchStatus(batch.getPayStatus());
        vo.setOutTradeNo(batch.getOutTradeNo());
        vo.setTradeNo(batch.getTradeNo());
        vo.setPayTime(batch.getPayTime());

        List<OrderBatchPayStatusVO.OrderStatusItem> orderStatusItems = new ArrayList<>();
        for (SysOrderPayBatchItem item : items) {
            SysOrder order = orderMap.get(item.getOrderId());
            OrderBatchPayStatusVO.OrderStatusItem statusItem = new OrderBatchPayStatusVO.OrderStatusItem();
            statusItem.setOrderId(item.getOrderId());
            statusItem.setOrderSn(item.getOrderSn());
            statusItem.setOrderStatus(order == null ? null : order.getOrderStatus());
            orderStatusItems.add(statusItem);
        }
        vo.setOrders(orderStatusItems);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderBatchPayStatusVO syncBatchPayStatus(Long batchId, Long userId) {
        SysOrderPayBatch batch = getBatchOrThrow(batchId);
        if (!Objects.equals(batch.getUserId(), userId)) {
            throw new BusinessException("无权同步该支付状态");
        }
        if (Objects.equals(batch.getPayStatus(), PaymentStatus.SUCCESS.getCode())) {
            return queryBatchPayStatus(batchId, userId);
        }

        AlipayTradeQueryResponse queryResponse = queryAlipayTrade(batch.getOutTradeNo());
        if (queryResponse == null) {
            return queryBatchPayStatus(batchId, userId);
        }

        String tradeStatus = queryResponse.getTradeStatus();
        String tradeNo = queryResponse.getTradeNo();
        String totalAmount = queryResponse.getTotalAmount();
        if (StringUtils.hasText(totalAmount)) {
            BigDecimal notifyAmount = normalizeAmount(parseAmount(totalAmount));
            BigDecimal recordAmount = normalizeAmount(batch.getTotalAmount());
            if (notifyAmount.compareTo(recordAmount) != 0) {
                throw new BusinessException("支付金额校验失败");
            }
        }

        Map<String, String> querySnapshot = new HashMap<>();
        querySnapshot.put("source", "query");
        querySnapshot.put("out_trade_no", batch.getOutTradeNo());
        querySnapshot.put("trade_no", tradeNo);
        querySnapshot.put("trade_status", tradeStatus);
        querySnapshot.put("total_amount", totalAmount);
        applyBatchTradeStatus(batch, tradeStatus, tradeNo, null, toJson(querySnapshot));
        return queryBatchPayStatus(batchId, userId);
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

        SysOrderPayment payment = orderPaymentRepository.selectOne(
                new LambdaQueryWrapper<SysOrderPayment>()
                        .eq(SysOrderPayment::getOutTradeNo, outTradeNo)
                        .last("limit 1")
        );
        if (payment == null) {
            SysOrderPayBatch batch = orderPayBatchRepository.selectOne(
                    new LambdaQueryWrapper<SysOrderPayBatch>()
                            .eq(SysOrderPayBatch::getOutTradeNo, outTradeNo)
                            .last("limit 1")
            );
            if (batch == null) {
                throw new BusinessException("支付记录不存在: " + outTradeNo);
            }

            if (Objects.equals(batch.getPayStatus(), PaymentStatus.SUCCESS.getCode())) {
                updateBatchNotifyOnly(batch.getId(), notifyParams);
                return true;
            }

            String tradeNo = notifyParams.get("trade_no");
            if (("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus))
                    && !StringUtils.hasText(tradeNo)) {
                throw new BusinessException("回调缺少 trade_no");
            }
            String totalAmount = notifyParams.get("total_amount");
            BigDecimal notifyAmount = normalizeAmount(parseAmount(totalAmount));
            BigDecimal recordAmount = normalizeAmount(batch.getTotalAmount());
            if (notifyAmount.compareTo(recordAmount) != 0) {
                throw new BusinessException("支付金额校验失败");
            }
            LocalDateTime paidTime = parseAlipayTime(notifyParams.get("gmt_payment"));
            applyBatchTradeStatus(batch, tradeStatus, tradeNo, paidTime, toJson(notifyParams));
            return true;
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
        String totalAmount = notifyParams.get("total_amount");
        BigDecimal notifyAmount = normalizeAmount(parseAmount(totalAmount));
        BigDecimal recordAmount = normalizeAmount(payment.getTotalAmount());
        if (notifyAmount.compareTo(recordAmount) != 0) {
            throw new BusinessException("支付金额校验失败");
        }

        SysOrder order = getOrderOrThrow(payment.getOrderId());
        LocalDateTime paidTime = parseAlipayTime(notifyParams.get("gmt_payment"));

        applyTradeStatus(order, payment, tradeStatus, tradeNo, paidTime, toJson(notifyParams));
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeTimeoutPayments() {
        int timeoutMinutes = paymentProperties.getTimeoutMinutes() == null ? 30 : paymentProperties.getTimeoutMinutes();
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeoutMinutes);

        List<SysOrderPayment> timeoutPayments = orderPaymentRepository.selectList(
                new LambdaQueryWrapper<SysOrderPayment>()
                        .eq(SysOrderPayment::getPayStatus, PaymentStatus.PENDING.getCode())
                        .le(SysOrderPayment::getCreateTime, threshold)
        );

        if (timeoutPayments == null || timeoutPayments.isEmpty()) {
            return;
        }

        for (SysOrderPayment payment : timeoutPayments) {
            SysOrder order = orderRepository.selectById(payment.getOrderId());
            if (order == null) {
                continue;
            }
            if (!Objects.equals(order.getOrderStatus(), OrderStatus.PENDING_PAYMENT.getCode())) {
                continue;
            }

            int orderUpdated = orderRepository.update(null,
                    new LambdaUpdateWrapper<SysOrder>()
                            .eq(SysOrder::getId, order.getId())
                            .eq(SysOrder::getOrderStatus, OrderStatus.PENDING_PAYMENT.getCode())
                            .set(SysOrder::getOrderStatus, OrderStatus.CANCELED.getCode())
            );
            if (orderUpdated <= 0) {
                continue;
            }

            orderPaymentRepository.update(null,
                    new LambdaUpdateWrapper<SysOrderPayment>()
                            .eq(SysOrderPayment::getId, payment.getId())
                            .eq(SysOrderPayment::getPayStatus, PaymentStatus.PENDING.getCode())
                            .set(SysOrderPayment::getPayStatus, PaymentStatus.CLOSED.getCode())
                            .set(SysOrderPayment::getCloseTime, LocalDateTime.now())
            );
        }

        List<SysOrderPayBatch> timeoutBatches = orderPayBatchRepository.selectList(
                new LambdaQueryWrapper<SysOrderPayBatch>()
                        .eq(SysOrderPayBatch::getPayStatus, PaymentStatus.PENDING.getCode())
                        .le(SysOrderPayBatch::getCreateTime, threshold)
        );
        if (timeoutBatches == null || timeoutBatches.isEmpty()) {
            return;
        }

        for (SysOrderPayBatch batch : timeoutBatches) {
            orderPayBatchRepository.update(null,
                    new LambdaUpdateWrapper<SysOrderPayBatch>()
                            .eq(SysOrderPayBatch::getId, batch.getId())
                            .eq(SysOrderPayBatch::getPayStatus, PaymentStatus.PENDING.getCode())
                            .set(SysOrderPayBatch::getPayStatus, PaymentStatus.CLOSED.getCode())
                            .set(SysOrderPayBatch::getCloseTime, LocalDateTime.now())
            );

            List<SysOrderPayBatchItem> items = getBatchItems(batch.getId());
            for (SysOrderPayBatchItem item : items) {
                orderRepository.update(null,
                        new LambdaUpdateWrapper<SysOrder>()
                                .eq(SysOrder::getId, item.getOrderId())
                                .eq(SysOrder::getOrderStatus, OrderStatus.PENDING_PAYMENT.getCode())
                                .set(SysOrder::getOrderStatus, OrderStatus.CANCELED.getCode())
                );
            }
        }
    }

    private String buildAppPayOrderString(SysOrder order, SysOrderPayment payment, BigDecimal amount) {
        try {
            AlipayClient alipayClient = buildClient();
            AlipayTradeAppPayRequest alipayRequest = new AlipayTradeAppPayRequest();

            AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
            model.setOutTradeNo(payment.getOutTradeNo());
            model.setProductCode(APP_PAY_PRODUCT_CODE);
            model.setTotalAmount(amount.setScale(2, RoundingMode.HALF_UP).toPlainString());
            model.setSubject("3DShop订单-" + order.getOrderSn());
            model.setBody("3DShop 模型定制订单支付");
            model.setTimeoutExpress(resolveTimeoutExpress());
            alipayRequest.setBizModel(model);
            alipayRequest.setNotifyUrl(paymentProperties.getNotifyUrl());
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

    private String buildBatchAppPayOrderString(SysOrderPayBatch batch, int orderCount) {
        try {
            AlipayClient alipayClient = buildClient();
            AlipayTradeAppPayRequest alipayRequest = new AlipayTradeAppPayRequest();

            AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
            model.setOutTradeNo(batch.getOutTradeNo());
            model.setProductCode(APP_PAY_PRODUCT_CODE);
            model.setTotalAmount(normalizeAmount(batch.getTotalAmount()).toPlainString());
            model.setSubject("3DShop合并支付-" + orderCount + "笔");
            model.setBody("3DShop 多订单合并支付");
            model.setTimeoutExpress(resolveTimeoutExpress());
            alipayRequest.setBizModel(model);
            alipayRequest.setNotifyUrl(paymentProperties.getNotifyUrl());
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
            log.error("支付宝回调验签异常", e);
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
            log.warn("主动查单失败 outTradeNo={}", outTradeNo, ex);
            return null;
        }
    }

    private void applyTradeStatus(SysOrder order,
                                  SysOrderPayment payment,
                                  String tradeStatus,
                                  String tradeNo,
                                  LocalDateTime paidTime,
                                  String notifyContent) {
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            orderPaymentRepository.update(null,
                    new LambdaUpdateWrapper<SysOrderPayment>()
                            .eq(SysOrderPayment::getId, payment.getId())
                            .in(SysOrderPayment::getPayStatus, PaymentStatus.PENDING.getCode(), PaymentStatus.FAILED.getCode(), PaymentStatus.CLOSED.getCode())
                            .set(SysOrderPayment::getPayStatus, PaymentStatus.SUCCESS.getCode())
                            .set(SysOrderPayment::getTradeNo, tradeNo)
                            .set(SysOrderPayment::getPayTime, paidTime == null ? LocalDateTime.now() : paidTime)
                            .set(SysOrderPayment::getNotifyContent, notifyContent)
            );

            if (Objects.equals(order.getOrderStatus(), OrderStatus.PENDING_PAYMENT.getCode())
                    || Objects.equals(order.getOrderStatus(), OrderStatus.CANCELED.getCode())) {
                int updated = orderRepository.update(null,
                        new LambdaUpdateWrapper<SysOrder>()
                                .eq(SysOrder::getId, order.getId())
                                .set(SysOrder::getOrderStatus, OrderStatus.IN_PRODUCTION.getCode())
                );
                if (updated > 0) {
                    pointService.rewardOrderPaid(order.getUserId(), order.getId(), order.getOrderSn(), order.getOrderPrice());
                    // 环保材料积分奖励
                    rewardEcoMaterialIfApplicable(order);
                    applicationEventPublisher.publishEvent(new OrderPaidEvent(order.getId()));
                }
            }
            return;
        }

        if ("TRADE_CLOSED".equals(tradeStatus)) {
            orderPaymentRepository.update(null,
                    new LambdaUpdateWrapper<SysOrderPayment>()
                            .eq(SysOrderPayment::getId, payment.getId())
                            .set(SysOrderPayment::getPayStatus, PaymentStatus.CLOSED.getCode())
                            .set(SysOrderPayment::getCloseTime, LocalDateTime.now())
                            .set(SysOrderPayment::getNotifyContent, notifyContent)
            );
            if (Objects.equals(order.getOrderStatus(), OrderStatus.PENDING_PAYMENT.getCode())) {
                int usedPoints = parseUsedPointsFromCustomParams(order.getCustomParams());
                if (usedPoints > 0) {
                    pointService.refundOrderPoints(order.getUserId(), order.getId(), order.getOrderSn(), usedPoints);
                }
                orderRepository.update(null,
                        new LambdaUpdateWrapper<SysOrder>()
                                .eq(SysOrder::getId, order.getId())
                                .eq(SysOrder::getOrderStatus, OrderStatus.PENDING_PAYMENT.getCode())
                                .set(SysOrder::getOrderStatus, OrderStatus.CANCELED.getCode())
                );
            }
            return;
        }

        if (StringUtils.hasText(notifyContent)) {
            orderPaymentRepository.update(null,
                new LambdaUpdateWrapper<SysOrderPayment>()
                    .eq(SysOrderPayment::getId, payment.getId())
                    .set(SysOrderPayment::getNotifyContent, notifyContent)
            );
            return;
        }

        updatePaymentNotifyOnly(payment.getId(), Collections.singletonMap("tradeStatus", tradeStatus));
    }

    private void applyBatchTradeStatus(SysOrderPayBatch batch,
                                       String tradeStatus,
                                       String tradeNo,
                                       LocalDateTime paidTime,
                                       String notifyContent) {
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            orderPayBatchRepository.update(null,
                    new LambdaUpdateWrapper<SysOrderPayBatch>()
                            .eq(SysOrderPayBatch::getId, batch.getId())
                            .in(SysOrderPayBatch::getPayStatus, PaymentStatus.PENDING.getCode(), PaymentStatus.FAILED.getCode(), PaymentStatus.CLOSED.getCode())
                            .set(SysOrderPayBatch::getPayStatus, PaymentStatus.SUCCESS.getCode())
                            .set(SysOrderPayBatch::getTradeNo, tradeNo)
                            .set(SysOrderPayBatch::getPayTime, paidTime == null ? LocalDateTime.now() : paidTime)
                            .set(SysOrderPayBatch::getNotifyContent, notifyContent)
            );

            List<SysOrderPayBatchItem> items = getBatchItems(batch.getId());
            for (SysOrderPayBatchItem item : items) {
                SysOrder order = orderRepository.selectById(item.getOrderId());
                int updated = orderRepository.update(null,
                        new LambdaUpdateWrapper<SysOrder>()
                                .eq(SysOrder::getId, item.getOrderId())
                                .in(SysOrder::getOrderStatus, OrderStatus.PENDING_PAYMENT.getCode(), OrderStatus.CANCELED.getCode())
                                .set(SysOrder::getOrderStatus, OrderStatus.IN_PRODUCTION.getCode())
                );
                if (updated > 0) {
                    if (order != null) {
                        pointService.rewardOrderPaid(order.getUserId(), order.getId(), order.getOrderSn(), order.getOrderPrice());
                        // 环保材料积分奖励
                        rewardEcoMaterialIfApplicable(order);
                    }
                    applicationEventPublisher.publishEvent(new OrderPaidEvent(item.getOrderId()));
                }
            }
            return;
        }

        if ("TRADE_CLOSED".equals(tradeStatus)) {
            orderPayBatchRepository.update(null,
                    new LambdaUpdateWrapper<SysOrderPayBatch>()
                            .eq(SysOrderPayBatch::getId, batch.getId())
                            .set(SysOrderPayBatch::getPayStatus, PaymentStatus.CLOSED.getCode())
                            .set(SysOrderPayBatch::getCloseTime, LocalDateTime.now())
                            .set(SysOrderPayBatch::getNotifyContent, notifyContent)
            );

            List<SysOrderPayBatchItem> items = getBatchItems(batch.getId());
            for (SysOrderPayBatchItem item : items) {
                SysOrder order = orderRepository.selectById(item.getOrderId());
                if (order != null && Objects.equals(order.getOrderStatus(), OrderStatus.PENDING_PAYMENT.getCode())) {
                    int usedPoints = parseUsedPointsFromCustomParams(order.getCustomParams());
                    if (usedPoints > 0) {
                        pointService.refundOrderPoints(order.getUserId(), order.getId(), order.getOrderSn(), usedPoints);
                    }
                }
                orderRepository.update(null,
                        new LambdaUpdateWrapper<SysOrder>()
                                .eq(SysOrder::getId, item.getOrderId())
                                .eq(SysOrder::getOrderStatus, OrderStatus.PENDING_PAYMENT.getCode())
                                .set(SysOrder::getOrderStatus, OrderStatus.CANCELED.getCode())
                );
            }
            return;
        }

        if (StringUtils.hasText(notifyContent)) {
            orderPayBatchRepository.update(null,
                    new LambdaUpdateWrapper<SysOrderPayBatch>()
                            .eq(SysOrderPayBatch::getId, batch.getId())
                            .set(SysOrderPayBatch::getNotifyContent, notifyContent)
            );
            return;
        }

        updateBatchNotifyOnly(batch.getId(), Collections.singletonMap("tradeStatus", tradeStatus));
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

    private int parseUsedPointsFromCustomParams(String customParams) {
        if (!StringUtils.hasText(customParams)) {
            return 0;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(customParams);
            com.fasterxml.jackson.databind.JsonNode pointsNode = node.get("usedPoints");
            if (pointsNode == null || pointsNode.isNull()) {
                return 0;
            }
            int points = pointsNode.asInt(0);
            return Math.max(points, 0);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String resolveTimeoutExpress() {
        int timeoutMinutes = paymentProperties.getTimeoutMinutes() == null ? 30 : paymentProperties.getTimeoutMinutes();
        return Math.max(timeoutMinutes, 1) + "m";
    }

    private SysOrder getOrderOrThrow(Long orderId) {
        SysOrder order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private SysOrderPayment getLatestPayment(Long orderId) {
        return orderPaymentRepository.selectOne(
                new LambdaQueryWrapper<SysOrderPayment>()
                        .eq(SysOrderPayment::getOrderId, orderId)
                        .orderByDesc(SysOrderPayment::getCreateTime)
                        .last("limit 1")
        );
    }

    private SysOrderPayment getLatestPaymentByOrderId(Long orderId, Integer payStatus) {
        return orderPaymentRepository.selectOne(
                new LambdaQueryWrapper<SysOrderPayment>()
                        .eq(SysOrderPayment::getOrderId, orderId)
                        .eq(SysOrderPayment::getPayStatus, payStatus)
                        .orderByDesc(SysOrderPayment::getCreateTime)
                        .last("limit 1")
        );
    }

    private SysOrderPayBatch getBatchOrThrow(Long batchId) {
        SysOrderPayBatch batch = orderPayBatchRepository.selectById(batchId);
        if (batch == null) {
            throw new BusinessException("合并支付记录不存在");
        }
        return batch;
    }

    private List<SysOrderPayBatchItem> getBatchItems(Long batchId) {
        return orderPayBatchItemRepository.selectList(
                new LambdaQueryWrapper<SysOrderPayBatchItem>()
                        .eq(SysOrderPayBatchItem::getBatchId, batchId)
                        .orderByAsc(SysOrderPayBatchItem::getCreateTime)
        );
    }

    private Map<Long, SysOrder> loadOrderMap(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysOrder> orders = orderRepository.selectBatchIds(orderIds);
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyMap();
        }
        return orders.stream().collect(Collectors.toMap(SysOrder::getId, item -> item));
    }

    private String generateOutTradeNo(Long orderId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String orderSuffix = String.valueOf(orderId);
        if (orderSuffix.length() > 10) {
            orderSuffix = orderSuffix.substring(orderSuffix.length() - 10);
        }
        return "AP" + timestamp + random + orderSuffix;
    }

    private String generateBatchOutTradeNo(Long userId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String userSuffix = String.valueOf(userId == null ? 0L : userId);
        if (userSuffix.length() > 8) {
            userSuffix = userSuffix.substring(userSuffix.length() - 8);
        }
        return "BP" + timestamp + random + userSuffix;
    }

    private String generateWalletOutTradeNo(Long orderId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String orderSuffix = String.valueOf(orderId == null ? 0L : orderId);
        if (orderSuffix.length() > 10) {
            orderSuffix = orderSuffix.substring(orderSuffix.length() - 10);
        }
        return "WP" + timestamp + random + orderSuffix;
    }

    private String generateWalletBatchOutTradeNo(Long userId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String userSuffix = String.valueOf(userId == null ? 0L : userId);
        if (userSuffix.length() > 8) {
            userSuffix = userSuffix.substring(userSuffix.length() - 8);
        }
        return "WB" + timestamp + random + userSuffix;
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

    private void debitWalletForOrder(WalletAccount wallet, SysOrder order, BigDecimal amount) {
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
        ledger.setUserId(order.getUserId());
        ledger.setAccountId(wallet.getId());
        ledger.setDirection(WalletLedgerDirection.EXPENSE.getCode());
        ledger.setBizType("ORDER_PAY_BALANCE");
        ledger.setBizNo(String.valueOf(order.getId()));
        ledger.setRefId(order.getId());
        ledger.setAmount(amount);
        ledger.setBeforeAvailable(beforeAvailable);
        ledger.setAfterAvailable(afterAvailable);
        ledger.setBeforeFrozen(beforeFrozen);
        ledger.setAfterFrozen(beforeFrozen);
        ledger.setRemark("订单余额支付");
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

    private void updatePaymentNotifyOnly(Long paymentId, Map<String, String> notifyParams) {
        orderPaymentRepository.update(null,
                new LambdaUpdateWrapper<SysOrderPayment>()
                        .eq(SysOrderPayment::getId, paymentId)
                        .set(SysOrderPayment::getNotifyContent, toJson(notifyParams))
        );
    }

    private void updateBatchNotifyOnly(Long batchId, Map<String, String> notifyParams) {
        orderPayBatchRepository.update(null,
                new LambdaUpdateWrapper<SysOrderPayBatch>()
                        .eq(SysOrderPayBatch::getId, batchId)
                        .set(SysOrderPayBatch::getNotifyContent, toJson(notifyParams))
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

    private void rewardEcoMaterialIfApplicable(SysOrder order) {
        if (order == null || order.getMaterialId() == null) {
            return;
        }
        try {
            ModelMaterial material = modelMaterialRepository.selectById(order.getMaterialId());
            if (material != null && Boolean.TRUE.equals(material.getIsEco())) {
                pointService.rewardEcoMaterial(order.getUserId(), order.getId(), order.getOrderSn());
            }
        } catch (Exception ex) {
            log.warn("环保材料积分奖励失败 orderId={}", order.getId(), ex);
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
}
