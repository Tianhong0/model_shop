package org.majun.backend.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.CertAlipayRequest;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.ResultCode;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.config.PaymentProperties;
import org.majun.backend.dto.AfterSaleAuditRequest;
import org.majun.backend.dto.AfterSaleCreateRequest;
import org.majun.backend.dto.AfterSaleMessageQueryRequest;
import org.majun.backend.dto.AfterSaleMessageSendRequest;
import org.majun.backend.dto.AfterSaleQueryRequest;
import org.majun.backend.dto.AfterSaleRefundRequest;
import org.majun.backend.entity.SysAfterSaleMessage;
import org.majun.backend.entity.SysOrder;
import org.majun.backend.entity.SysOrderAfterSale;
import org.majun.backend.entity.SysOrderPayment;
import org.majun.backend.enums.AfterSaleRefundStatus;
import org.majun.backend.enums.AfterSaleStatus;
import org.majun.backend.enums.AfterSaleType;
import org.majun.backend.enums.OrderStatus;
import org.majun.backend.enums.PaymentStatus;
import org.majun.backend.repository.SysAfterSaleMessageRepository;
import org.majun.backend.repository.SysOrderAfterSaleRepository;
import org.majun.backend.repository.SysOrderPaymentRepository;
import org.majun.backend.service.AfterSaleWebSocketService;
import org.majun.backend.repository.SysOrderRepository;
import org.majun.backend.service.OrderAfterSaleService;
import org.majun.backend.vo.AfterSaleDetailVO;
import org.majun.backend.vo.AfterSaleListVO;
import org.majun.backend.vo.AfterSaleMessageVO;
import org.majun.backend.vo.PageResult;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/**
 * 订单售后服务实现
 */
public class OrderAfterSaleServiceImpl extends ServiceImpl<SysOrderAfterSaleRepository, SysOrderAfterSale> implements OrderAfterSaleService {

    private static final DateTimeFormatter AFTER_SALE_SN_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final SysOrderAfterSaleRepository afterSaleRepository;
    private final SysAfterSaleMessageRepository afterSaleMessageRepository;
    private final SysOrderRepository orderRepository;
    private final SysOrderPaymentRepository orderPaymentRepository;
    private final AfterSaleWebSocketService afterSaleWebSocketService;
    private final PaymentProperties paymentProperties;
    private final ResourceLoader resourceLoader;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAfterSale(AfterSaleCreateRequest request, Long userId) {
        SysOrder order = getOrderOrThrow(request.getOrderId());
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权申请该订单售后");
        }

        validateAfterSaleTypeAndOrderStatus(request.getType(), order.getOrderStatus());

        SysOrderAfterSale existingAfterSale = afterSaleRepository.selectOne(new LambdaQueryWrapper<SysOrderAfterSale>()
                .eq(SysOrderAfterSale::getOrderId, order.getId())
                .eq(SysOrderAfterSale::getIsDelete, 0)
                .orderByDesc(SysOrderAfterSale::getUpdateTime)
                .last("limit 1"));
        if (existingAfterSale != null && !Objects.equals(existingAfterSale.getStatus(), AfterSaleStatus.CANCELED.getCode())) {
            throw new BusinessException("每个订单仅允许提交一次售后申请");
        }

        BigDecimal requestedAmount = normalizeAmount(request.getRequestedAmount());
        if (requestedAmount == null) {
            requestedAmount = normalizeAmount(order.getOrderPrice());
        }

        SysOrderAfterSale afterSale = existingAfterSale != null ? existingAfterSale : new SysOrderAfterSale();
        afterSale.setAfterSaleSn(generateAfterSaleSn());
        afterSale.setOrderId(order.getId());
        afterSale.setOrderSn(order.getOrderSn());
        afterSale.setUserId(userId);
        afterSale.setType(request.getType());
        afterSale.setReason(StringUtils.trimWhitespace(request.getReason()));
        afterSale.setDescription(StringUtils.trimWhitespace(request.getDescription()));
        afterSale.setEvidenceUrls(StringUtils.trimWhitespace(request.getEvidenceUrls()));
        afterSale.setRequestedAmount(requestedAmount);
        afterSale.setApprovedAmount(null);
        afterSale.setStatus(AfterSaleStatus.APPLIED.getCode());
        afterSale.setRefundStatus(AfterSaleRefundStatus.NONE.getCode());
        afterSale.setAdminRemark(null);
        afterSale.setCloseReason(null);
        afterSale.setIsDelete(0);

        if (existingAfterSale != null) {
            afterSaleRepository.updateById(afterSale);
        } else {
            afterSaleRepository.insert(afterSale);
        }

        appendSystemMessage(afterSale.getId(), "USER", userId, "用户发起售后申请");
        return afterSale.getId();
    }

    @Override
    public PageResult<AfterSaleListVO> pageMyAfterSales(AfterSaleQueryRequest request, Long userId) {
        return pageAfterSales(request, userId);
    }

    @Override
    public PageResult<AfterSaleListVO> pageAdminAfterSales(AfterSaleQueryRequest request) {
        return pageAfterSales(request, null);
    }

    @Override
    public AfterSaleDetailVO getMyAfterSaleDetail(Long afterSaleId, Long userId) {
        SysOrderAfterSale afterSale = getAfterSaleOrThrow(afterSaleId);
        if (!Objects.equals(afterSale.getUserId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权查看该售后单");
        }
        return toDetailVO(afterSale, 20);
    }

    @Override
    public AfterSaleDetailVO getMyAfterSaleDetailBySn(String afterSaleSn, Long userId) {
        SysOrderAfterSale afterSale = getAfterSaleBySnOrThrow(afterSaleSn);
        if (!Objects.equals(afterSale.getUserId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权查看该售后单");
        }
        return toDetailVO(afterSale, 20);
    }

    @Override
    public AfterSaleDetailVO getAdminAfterSaleDetail(Long afterSaleId) {
        return toDetailVO(getAfterSaleOrThrow(afterSaleId), 50);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAfterSale(Long afterSaleId, Long userId) {
        SysOrderAfterSale afterSale = getAfterSaleOrThrow(afterSaleId);
        if (!Objects.equals(afterSale.getUserId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权取消该售后单");
        }
        if (!Objects.equals(afterSale.getStatus(), AfterSaleStatus.APPLIED.getCode())
                && !Objects.equals(afterSale.getStatus(), AfterSaleStatus.REVIEWING.getCode())) {
            throw new BusinessException("当前状态不可取消");
        }
        afterSale.setStatus(AfterSaleStatus.CANCELED.getCode());
        afterSale.setCloseReason("用户主动取消");
        afterSaleRepository.updateById(afterSale);
        appendSystemMessage(afterSaleId, "USER", userId, "用户取消了售后申请");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAfterSaleBySn(String afterSaleSn, Long userId) {
        SysOrderAfterSale afterSale = getAfterSaleBySnOrThrow(afterSaleSn);
        if (!Objects.equals(afterSale.getUserId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权取消该售后单");
        }
        if (!Objects.equals(afterSale.getStatus(), AfterSaleStatus.APPLIED.getCode())
                && !Objects.equals(afterSale.getStatus(), AfterSaleStatus.REVIEWING.getCode())) {
            throw new BusinessException("当前状态不可取消");
        }
        afterSale.setStatus(AfterSaleStatus.CANCELED.getCode());
        afterSale.setCloseReason("用户主动取消");
        afterSaleRepository.updateById(afterSale);
        appendSystemMessage(afterSale.getId(), "USER", userId, "用户取消了售后申请");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditAfterSale(AfterSaleAuditRequest request) {
        SysOrderAfterSale afterSale = getAfterSaleOrThrow(request.getAfterSaleId());
        if (!Objects.equals(afterSale.getStatus(), AfterSaleStatus.APPLIED.getCode())
                && !Objects.equals(afterSale.getStatus(), AfterSaleStatus.REVIEWING.getCode())) {
            throw new BusinessException("当前售后单状态不支持审核");
        }

        String adminRemark = StringUtils.trimWhitespace(request.getAdminRemark());
        if (Boolean.TRUE.equals(request.getApproved())) {
            afterSale.setApprovedAmount(normalizeAmount(request.getApprovedAmount()));
            afterSale.setAdminRemark(adminRemark);
            if (isRefundType(afterSale.getType())) {
                afterSale.setStatus(AfterSaleStatus.REFUNDING.getCode());
                afterSale.setRefundStatus(AfterSaleRefundStatus.PENDING.getCode());
            } else {
                afterSale.setStatus(AfterSaleStatus.PROCESSING.getCode());
                afterSale.setRefundStatus(AfterSaleRefundStatus.NONE.getCode());
            }
            appendSystemMessage(afterSale.getId(), "ADMIN", 0L, "管理员审核通过");
        } else {
            afterSale.setStatus(AfterSaleStatus.REJECTED.getCode());
            afterSale.setAdminRemark(adminRemark);
            afterSale.setCloseReason(StringUtils.hasText(adminRemark) ? adminRemark : "管理员审核拒绝");
            appendSystemMessage(afterSale.getId(), "ADMIN", 0L, "管理员审核拒绝");
        }

        afterSaleRepository.updateById(afterSale);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeRefund(AfterSaleRefundRequest request) {
        SysOrderAfterSale afterSale = getAfterSaleOrThrow(request.getAfterSaleId());
        if (!isRefundType(afterSale.getType())) {
            throw new BusinessException("当前售后类型不支持退款");
        }
        if (!Objects.equals(afterSale.getStatus(), AfterSaleStatus.REFUNDING.getCode())) {
            throw new BusinessException("当前售后单未进入退款流程");
        }

        SysOrder order = getOrderOrThrow(afterSale.getOrderId());
        SysOrderPayment payment = getLatestPaidRecord(order.getId());
        if (payment == null) {
            throw new BusinessException("未找到可退款的支付记录");
        }

        String tradeStatus = queryTradeStatus(payment);
        if ("TRADE_CLOSED".equals(tradeStatus)) {
            afterSale.setRefundStatus(AfterSaleRefundStatus.FAILED.getCode());
            afterSale.setAdminRemark("支付宝交易已关闭，无法退款");
            afterSaleRepository.updateById(afterSale);
            throw new BusinessException("支付宝交易已关闭，无法退款（沙箱请使用新支付订单重试）");
        }
        if ("WAIT_BUYER_PAY".equals(tradeStatus)) {
            afterSale.setRefundStatus(AfterSaleRefundStatus.FAILED.getCode());
            afterSale.setAdminRemark("订单未支付成功，无法退款");
            afterSaleRepository.updateById(afterSale);
            throw new BusinessException("订单未支付成功，无法退款");
        }

        BigDecimal refundAmount = normalizeAmount(request.getRefundAmount());
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("退款金额非法");
        }
        if (payment.getTotalAmount() != null && refundAmount.compareTo(payment.getTotalAmount()) > 0) {
            throw new BusinessException("退款金额不能大于已支付金额");
        }

        String outRequestNo = "ASRF" + afterSale.getId() + System.currentTimeMillis();
        AlipayTradeRefundResponse response = callAlipayRefund(payment, refundAmount, request.getRefundReason(), outRequestNo);

        if (response == null || !response.isSuccess()) {
            afterSale.setRefundStatus(AfterSaleRefundStatus.FAILED.getCode());
            afterSaleRepository.updateById(afterSale);
            throw new BusinessException("支付宝退款失败: " + (response == null ? "unknown" : response.getSubMsg()));
        }

        afterSale.setApprovedAmount(refundAmount);
        afterSale.setRefundStatus(AfterSaleRefundStatus.SUCCESS.getCode());
        afterSale.setStatus(AfterSaleStatus.COMPLETED.getCode());
        if (StringUtils.hasText(response.getMsg())) {
            afterSale.setAdminRemark(response.getMsg());
        }
        afterSaleRepository.updateById(afterSale);
        appendSystemMessage(afterSale.getId(), "ADMIN", 0L, "管理员执行退款成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendMessage(AfterSaleMessageSendRequest request, Long senderId, String senderRole, boolean adminMode) {
        SysOrderAfterSale afterSale = getAfterSaleOrThrow(request.getAfterSaleId());
        if (!adminMode && !Objects.equals(afterSale.getUserId(), senderId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权在该售后单下留言");
        }

        SysAfterSaleMessage message = new SysAfterSaleMessage();
        message.setAfterSaleId(afterSale.getId());
        message.setSenderId(senderId);
        message.setSenderRole(senderRole);
        message.setMessageType(request.getMessageType() == null ? 1 : request.getMessageType());
        message.setContent(StringUtils.trimWhitespace(request.getContent()));
        message.setAttachments(StringUtils.trimWhitespace(request.getAttachments()));
        message.setIsSystem(0);
        message.setIsDelete(0);
        afterSaleMessageRepository.insert(message);
        afterSaleWebSocketService.broadcastMessage(afterSale.getId(), toMessageVO(message));
    }

    @Override
    public PageResult<AfterSaleMessageVO> pageMessages(AfterSaleMessageQueryRequest request, Long userId, boolean adminMode) {
        SysOrderAfterSale afterSale = getAfterSaleOrThrow(request.getAfterSaleId());
        if (!adminMode && !Objects.equals(afterSale.getUserId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权查看该售后单留言");
        }

        Page<SysAfterSaleMessage> page = new Page<>(request.getPageNum(), request.getPageSize());
        afterSaleMessageRepository.selectPage(page, new LambdaQueryWrapper<SysAfterSaleMessage>()
                .eq(SysAfterSaleMessage::getAfterSaleId, request.getAfterSaleId())
                .eq(SysAfterSaleMessage::getIsDelete, 0)
                .orderByDesc(SysAfterSaleMessage::getCreateTime)
        );

        List<AfterSaleMessageVO> records = page.getRecords().stream().map(this::toMessageVO).collect(Collectors.toList());

        return PageResult.<AfterSaleMessageVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    private PageResult<AfterSaleListVO> pageAfterSales(AfterSaleQueryRequest request, Long userId) {
        Page<SysOrderAfterSale> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<SysOrderAfterSale> wrapper = new LambdaQueryWrapper<SysOrderAfterSale>()
                .eq(SysOrderAfterSale::getIsDelete, 0)
                .orderByDesc(SysOrderAfterSale::getCreateTime);

        if (userId != null) {
            wrapper.eq(SysOrderAfterSale::getUserId, userId);
        }
        if (request.getStatus() != null) {
            wrapper.eq(SysOrderAfterSale::getStatus, request.getStatus());
        } else if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
            wrapper.in(SysOrderAfterSale::getStatus, request.getStatuses());
        }
        if (request.getType() != null) {
            wrapper.eq(SysOrderAfterSale::getType, request.getType());
        }
        if (StringUtils.hasText(request.getOrderSn())) {
            wrapper.like(SysOrderAfterSale::getOrderSn, request.getOrderSn().trim());
        }
        if (StringUtils.hasText(request.getAfterSaleSn())) {
            wrapper.like(SysOrderAfterSale::getAfterSaleSn, request.getAfterSaleSn().trim());
        }

        afterSaleRepository.selectPage(page, wrapper);

        List<AfterSaleListVO> records = page.getRecords().stream()
                .map(this::toListVO)
                .collect(Collectors.toList());

        return PageResult.<AfterSaleListVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    private AfterSaleDetailVO toDetailVO(SysOrderAfterSale afterSale, int latestSize) {
        AfterSaleDetailVO vo = new AfterSaleDetailVO();
        vo.setId(afterSale.getId());
        vo.setAfterSaleSn(afterSale.getAfterSaleSn());
        vo.setOrderId(afterSale.getOrderId());
        vo.setOrderSn(afterSale.getOrderSn());
        vo.setUserId(afterSale.getUserId());
        vo.setType(afterSale.getType());
        vo.setReason(afterSale.getReason());
        vo.setDescription(afterSale.getDescription());
        vo.setEvidenceUrls(afterSale.getEvidenceUrls());
        vo.setRequestedAmount(afterSale.getRequestedAmount());
        vo.setApprovedAmount(afterSale.getApprovedAmount());
        vo.setStatus(afterSale.getStatus());
        vo.setRefundStatus(afterSale.getRefundStatus());
        vo.setAdminRemark(afterSale.getAdminRemark());
        vo.setCloseReason(afterSale.getCloseReason());
        vo.setCreateTime(afterSale.getCreateTime());
        vo.setUpdateTime(afterSale.getUpdateTime());

        List<SysAfterSaleMessage> latestMessages = afterSaleMessageRepository.selectList(
                new LambdaQueryWrapper<SysAfterSaleMessage>()
                        .eq(SysAfterSaleMessage::getAfterSaleId, afterSale.getId())
                        .eq(SysAfterSaleMessage::getIsDelete, 0)
                        .orderByDesc(SysAfterSaleMessage::getCreateTime)
                        .last("limit " + latestSize)
        );
        vo.setLatestMessages(latestMessages.stream().map(this::toMessageVO).collect(Collectors.toList()));
        return vo;
    }

    private AfterSaleListVO toListVO(SysOrderAfterSale afterSale) {
        AfterSaleListVO vo = new AfterSaleListVO();
        vo.setId(afterSale.getId());
        vo.setAfterSaleSn(afterSale.getAfterSaleSn());
        vo.setOrderId(afterSale.getOrderId());
        vo.setOrderSn(afterSale.getOrderSn());
        vo.setType(afterSale.getType());
        vo.setReason(afterSale.getReason());
        vo.setStatus(afterSale.getStatus());
        vo.setRefundStatus(afterSale.getRefundStatus());
        vo.setRequestedAmount(afterSale.getRequestedAmount());
        vo.setApprovedAmount(afterSale.getApprovedAmount());
        vo.setAdminRemark(afterSale.getAdminRemark());
        vo.setCreateTime(afterSale.getCreateTime());
        vo.setUpdateTime(afterSale.getUpdateTime());
        return vo;
    }

    private AfterSaleMessageVO toMessageVO(SysAfterSaleMessage message) {
        AfterSaleMessageVO vo = new AfterSaleMessageVO();
        vo.setId(message.getId());
        vo.setAfterSaleId(message.getAfterSaleId());
        vo.setSenderId(message.getSenderId());
        vo.setSenderRole(message.getSenderRole());
        vo.setMessageType(message.getMessageType());
        vo.setContent(message.getContent());
        vo.setAttachments(message.getAttachments());
        vo.setIsSystem(message.getIsSystem());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    private SysOrderAfterSale getAfterSaleOrThrow(Long afterSaleId) {
        SysOrderAfterSale afterSale = afterSaleRepository.selectById(afterSaleId);
        if (afterSale == null || Objects.equals(afterSale.getIsDelete(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "售后单不存在");
        }
        return afterSale;
    }

    private SysOrderAfterSale getAfterSaleBySnOrThrow(String afterSaleSn) {
        if (!StringUtils.hasText(afterSaleSn)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "售后单号不能为空");
        }
        SysOrderAfterSale afterSale = afterSaleRepository.selectOne(new LambdaQueryWrapper<SysOrderAfterSale>()
                .eq(SysOrderAfterSale::getAfterSaleSn, afterSaleSn.trim())
                .eq(SysOrderAfterSale::getIsDelete, 0)
                .last("limit 1"));
        if (afterSale == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "售后单不存在");
        }
        return afterSale;
    }

    private SysOrder getOrderOrThrow(Long orderId) {
        SysOrder order = orderRepository.selectById(orderId);
        if (order == null || Objects.equals(order.getIsDelete(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private void validateAfterSaleTypeAndOrderStatus(Integer typeCode, Integer orderStatusCode) {
        AfterSaleType type = AfterSaleType.fromCode(typeCode);
        if (type == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "售后类型非法");
        }
        OrderStatus orderStatus = OrderStatus.fromCode(orderStatusCode);
        if (orderStatus == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单状态非法");
        }

        switch (type) {
            case REFUND_ONLY -> {
                if (orderStatus != OrderStatus.PENDING_PAYMENT
                        && orderStatus != OrderStatus.IN_PRODUCTION
                        && orderStatus != OrderStatus.WAIT_SHIPMENT) {
                    throw new BusinessException("当前订单状态不支持仅退款");
                }
            }
            case RETURN_REFUND -> {
                if (orderStatus != OrderStatus.COMPLETED) {
                    throw new BusinessException("退货退款仅支持已完成订单");
                }
            }
            case REPRINT, EXCHANGE -> {
                if (orderStatus != OrderStatus.WAIT_SHIPMENT && orderStatus != OrderStatus.COMPLETED) {
                    throw new BusinessException("当前订单状态不支持补打/换货");
                }
            }
            default -> throw new BusinessException(ResultCode.PARAM_ERROR, "售后类型非法");
        }
    }

    private boolean isRefundType(Integer type) {
        return Objects.equals(type, AfterSaleType.REFUND_ONLY.getCode())
                || Objects.equals(type, AfterSaleType.RETURN_REFUND.getCode());
    }

    private String generateAfterSaleSn() {
        String timePart = LocalDateTime.now().format(AFTER_SALE_SN_TIME_FORMATTER);
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "AS" + timePart + randomPart;
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private void appendSystemMessage(Long afterSaleId, String senderRole, Long senderId, String content) {
        SysAfterSaleMessage message = new SysAfterSaleMessage();
        message.setAfterSaleId(afterSaleId);
        message.setSenderId(senderId);
        message.setSenderRole(senderRole);
        message.setMessageType(3);
        message.setContent(content);
        message.setAttachments(null);
        message.setIsSystem(1);
        message.setIsDelete(0);
        afterSaleMessageRepository.insert(message);
        afterSaleWebSocketService.broadcastMessage(afterSaleId, toMessageVO(message));
    }

    private SysOrderPayment getLatestPaidRecord(Long orderId) {
        List<SysOrderPayment> list = orderPaymentRepository.selectList(new LambdaQueryWrapper<SysOrderPayment>()
                .eq(SysOrderPayment::getOrderId, orderId)
                .eq(SysOrderPayment::getPayStatus, PaymentStatus.SUCCESS.getCode())
                .eq(SysOrderPayment::getIsDelete, 0)
                .orderByDesc(SysOrderPayment::getPayTime)
                .last("limit 1")
        );
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    private AlipayTradeRefundResponse callAlipayRefund(SysOrderPayment payment,
                                                       BigDecimal refundAmount,
                                                       String refundReason,
                                                       String outRequestNo) {
        if (payment == null) {
            throw new BusinessException("支付记录不存在");
        }
        String reason = StringUtils.hasText(refundReason) ? refundReason : "售后退款";

        AlipayTradeRefundResponse lastResponse = null;
        String tradeNo = StringUtils.hasText(payment.getTradeNo()) ? payment.getTradeNo().trim() : null;
        String outTradeNo = StringUtils.hasText(payment.getOutTradeNo()) ? payment.getOutTradeNo().trim() : null;

        if (StringUtils.hasText(tradeNo)) {
            lastResponse = doRefundByIdentifier(true, tradeNo, refundAmount, reason, outRequestNo);
            if (lastResponse != null && lastResponse.isSuccess()) {
                return lastResponse;
            }
        }

        if (StringUtils.hasText(outTradeNo) && !outTradeNo.equals(tradeNo)) {
            AlipayTradeRefundResponse byOutTradeNo = doRefundByIdentifier(false, outTradeNo, refundAmount, reason, outRequestNo);
            if (byOutTradeNo != null && byOutTradeNo.isSuccess()) {
                return byOutTradeNo;
            }
            lastResponse = byOutTradeNo != null ? byOutTradeNo : lastResponse;
        }

        return lastResponse;
    }

    private AlipayTradeRefundResponse doRefundByIdentifier(boolean useTradeNo,
                                                           String identifier,
                                                           BigDecimal refundAmount,
                                                           String refundReason,
                                                           String outRequestNo) {
        try {
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            AlipayTradeRefundModel model = new AlipayTradeRefundModel();
            if (useTradeNo) {
                model.setTradeNo(identifier);
            } else {
                model.setOutTradeNo(identifier);
            }
            model.setRefundAmount(refundAmount.toPlainString());
            model.setRefundReason(refundReason);
            model.setOutRequestNo(outRequestNo);
            request.setBizModel(model);
            return buildClient().certificateExecute(request);
        } catch (Exception ex) {
            throw new BusinessException("调用支付宝退款异常", ex);
        }
    }

    private String queryTradeStatus(SysOrderPayment payment) {
        AlipayTradeQueryResponse byTradeNo = queryTrade(true, payment.getTradeNo());
        if (byTradeNo != null && byTradeNo.isSuccess() && StringUtils.hasText(byTradeNo.getTradeStatus())) {
            return byTradeNo.getTradeStatus();
        }

        AlipayTradeQueryResponse byOutTradeNo = queryTrade(false, payment.getOutTradeNo());
        if (byOutTradeNo != null && byOutTradeNo.isSuccess() && StringUtils.hasText(byOutTradeNo.getTradeStatus())) {
            return byOutTradeNo.getTradeStatus();
        }
        return null;
    }

    private AlipayTradeQueryResponse queryTrade(boolean useTradeNo, String identifier) {
        if (!StringUtils.hasText(identifier)) {
            return null;
        }
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            String bizContent = useTradeNo
                    ? String.format("{\"trade_no\":\"%s\"}", identifier.trim())
                    : String.format("{\"out_trade_no\":\"%s\"}", identifier.trim());
            request.setBizContent(bizContent);
            return buildClient().certificateExecute(request);
        } catch (Exception ex) {
            return null;
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

    private String resolveResourcePath(String location) {
        try {
            if (!StringUtils.hasText(location)) {
                throw new BusinessException("支付宝证书路径未配置");
            }
            String normalized = location;
            if (!normalized.startsWith("classpath:")
                    && !normalized.startsWith("file:")
                    && !normalized.startsWith("http://")
                    && !normalized.startsWith("https://")) {
                normalized = "classpath:" + normalized;
            }

            Resource resource = resourceLoader.getResource(normalized);
            if (!resource.exists()) {
                throw new BusinessException("支付宝证书文件不存在: " + location);
            }

            if (resource.isFile()) {
                return resource.getFile().getAbsolutePath();
            }

            Path tempPath = Files.createTempFile("alipay-cert-", "-" + resource.getFilename());
            tempPath.toFile().deleteOnExit();
            try (var inputStream = resource.getInputStream()) {
                Files.copy(inputStream, tempPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return tempPath.toAbsolutePath().toString();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("读取支付宝证书失败: " + location, ex);
        }
    }
}
