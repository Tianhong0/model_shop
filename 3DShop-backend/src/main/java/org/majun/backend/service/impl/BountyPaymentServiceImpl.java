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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.config.PaymentProperties;
import org.majun.backend.entity.BountyEscrow;
import org.majun.backend.entity.BountyMessage;
import org.majun.backend.entity.BountyPriceChange;
import org.majun.backend.entity.BountyTask;
import org.majun.backend.enums.BountyEscrowStatus;
import org.majun.backend.enums.BountyPriceChangeStatus;
import org.majun.backend.repository.BountyEscrowRepository;
import org.majun.backend.repository.BountyMessageRepository;
import org.majun.backend.repository.BountyPriceChangeRepository;
import org.majun.backend.repository.BountyTaskRepository;
import org.majun.backend.service.BountyPaymentService;
import org.majun.backend.service.BountyWebSocketService;
import org.majun.backend.vo.BountyMessageVO;
import org.majun.backend.vo.BountyPayCreateResponse;
import org.majun.backend.vo.BountyPayStatusVO;
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
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BountyPaymentServiceImpl implements BountyPaymentService {

    private static final String APP_PAY_PRODUCT_CODE = "QUICK_MSECURITY_PAY";
    private static final String APP_PAY_CHANNEL = "ALIPAY_APP";
    private static final DateTimeFormatter ALIPAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> ALIPAY_NOTIFY_TRADE_STATUS = Set.of("WAIT_BUYER_PAY", "TRADE_SUCCESS", "TRADE_FINISHED", "TRADE_CLOSED");

    private final PaymentProperties paymentProperties;
    private final ResourceLoader resourceLoader;
    private final BountyTaskRepository bountyTaskRepository;
    private final BountyPriceChangeRepository bountyPriceChangeRepository;
    private final BountyEscrowRepository bountyEscrowRepository;
    private final BountyMessageRepository bountyMessageRepository;
    private final BountyWebSocketService bountyWebSocketService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BountyPayCreateResponse createPriceIncreasePayOrder(Long priceChangeId, Long userId) {
        validateAlipayConfig();
        BountyPriceChange change = getPriceChangeOrThrow(priceChangeId);
        BountyTask task = getTaskOrThrow(change.getTaskId());
        if (!Objects.equals(task.getPublisherId(), userId)) {
            throw new BusinessException("仅发布者可发起补差支付");
        }
        if (!Objects.equals(change.getStatus(), BountyPriceChangeStatus.AGREED.getCode())) {
            throw new BusinessException("当前改价状态不可支付");
        }

        BigDecimal delta = increaseDelta(change);
        if (delta.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("当前改价无需补差支付");
        }
        if (amount(task.getFinalAmount()).compareTo(amount(change.getTargetAmount())) >= 0) {
            throw new BusinessException("该改价补差已完成");
        }

        BountyEscrow escrow = getEscrowOrThrow(task.getId());
        if (!StringUtils.hasText(escrow.getOutTradeNo())) {
            escrow.setOutTradeNo(buildPriceIncreaseOutTradeNo(change.getId()));
        }
        escrow.setPayBatchId(change.getId());
        escrow.setStatus(BountyEscrowStatus.WAIT_PAY.getCode());
        bountyEscrowRepository.updateById(escrow);

        String orderString = buildAppPayOrderString(task, change.getId(), escrow.getOutTradeNo(), delta);
        return new BountyPayCreateResponse(change.getId(), task.getId(), escrow.getOutTradeNo(), delta, orderString);
    }

    @Override
    public BountyPayStatusVO queryPriceIncreasePayStatus(Long priceChangeId, Long userId) {
        BountyPriceChange change = getPriceChangeOrThrow(priceChangeId);
        BountyTask task = getTaskOrThrow(change.getTaskId());
        if (!Objects.equals(task.getPublisherId(), userId)) {
            throw new BusinessException("无权查看该补差支付状态");
        }
        BountyEscrow escrow = getEscrowOrThrow(task.getId());
        return buildStatusVO(change, task, escrow);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BountyPayStatusVO syncPriceIncreasePayStatus(Long priceChangeId, Long userId) {
        BountyPriceChange change = getPriceChangeOrThrow(priceChangeId);
        BountyTask task = getTaskOrThrow(change.getTaskId());
        if (!Objects.equals(task.getPublisherId(), userId)) {
            throw new BusinessException("无权同步该补差支付状态");
        }
        return syncByChange(task, change);
    }

    @Override
    public BountyPayStatusVO queryPriceIncreasePayStatusByTask(Long taskId, Long userId, boolean adminMode) {
        BountyTask task = getTaskOrThrow(taskId);
        if (!adminMode && !Objects.equals(task.getPublisherId(), userId)) {
            throw new BusinessException("无权查看该补差支付状态");
        }
        BountyPriceChange change = getPayRelatedPriceChange(task.getId());
        if (change == null) {
            BountyPayStatusVO vo = new BountyPayStatusVO();
            vo.setTaskId(task.getId());
            vo.setCurrentTaskAmount(amount(task.getFinalAmount()));
            vo.setNeedPay(false);
            vo.setPaid(true);
            return vo;
        }
        BountyEscrow escrow = getEscrowOrThrow(task.getId());
        return buildStatusVO(change, task, escrow);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BountyPayStatusVO syncPriceIncreasePayStatusByTask(Long taskId, Long userId, boolean adminMode) {
        BountyTask task = getTaskOrThrow(taskId);
        if (!adminMode && !Objects.equals(task.getPublisherId(), userId)) {
            throw new BusinessException("无权同步该补差支付状态");
        }
        BountyPriceChange change = getPayRelatedPriceChange(task.getId());
        if (change == null) {
            BountyPayStatusVO vo = new BountyPayStatusVO();
            vo.setTaskId(task.getId());
            vo.setCurrentTaskAmount(amount(task.getFinalAmount()));
            vo.setNeedPay(false);
            vo.setPaid(true);
            return vo;
        }
        return syncByChange(task, change);
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
        if (StringUtils.hasText(paymentProperties.getSellerId()) && !Objects.equals(paymentProperties.getSellerId(), sellerId)) {
            throw new BusinessException("回调 sellerId 不匹配");
        }

        String outTradeNo = notifyParams.get("out_trade_no");
        String tradeStatus = notifyParams.get("trade_status");
        if (!StringUtils.hasText(outTradeNo) || !StringUtils.hasText(tradeStatus)) {
            throw new BusinessException("回调参数缺失");
        }
        if (!ALIPAY_NOTIFY_TRADE_STATUS.contains(tradeStatus)) {
            throw new BusinessException("回调 trade_status 非法");
        }

        BountyEscrow escrow = bountyEscrowRepository.selectOne(new LambdaQueryWrapper<BountyEscrow>()
                .eq(BountyEscrow::getOutTradeNo, outTradeNo)
                .eq(BountyEscrow::getIsDelete, 0)
                .last("limit 1"));
        if (escrow == null || escrow.getPayBatchId() == null) {
            return true;
        }

        BountyPriceChange change = getPriceChangeOrThrow(escrow.getPayBatchId());
        BountyTask task = getTaskOrThrow(change.getTaskId());

        String totalAmount = notifyParams.get("total_amount");
        BigDecimal notifyAmount = amount(parseAmount(totalAmount));
        BigDecimal expectAmount = increaseDelta(change);
        if (notifyAmount.compareTo(expectAmount) != 0) {
            throw new BusinessException("补差支付金额校验失败");
        }

        applyTradeStatus(task, change, escrow, tradeStatus, notifyParams.get("trade_no"), parseAlipayTime(notifyParams.get("gmt_payment")));
        return true;
    }

    private void applyTradeStatus(BountyTask task,
                                  BountyPriceChange change,
                                  BountyEscrow escrow,
                                  String tradeStatus,
                                  String tradeNo,
                                  LocalDateTime payTime) {
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            if (amount(task.getFinalAmount()).compareTo(amount(change.getTargetAmount())) >= 0) {
                return;
            }
            BigDecimal delta = increaseDelta(change);
            task.setFinalAmount(change.getTargetAmount());
            bountyTaskRepository.updateById(task);

            escrow.setEscrowAmount(amount(escrow.getEscrowAmount()).add(delta));
            escrow.setStatus(BountyEscrowStatus.ESCROWED.getCode());
            escrow.setOutTradeNo(null);
            escrow.setPayBatchId(null);
            bountyEscrowRepository.updateById(escrow);

            sendSystemMessage(task.getId(), "改价补差支付成功，任务金额已生效");
            return;
        }

        if ("TRADE_CLOSED".equals(tradeStatus)) {
            escrow.setStatus(BountyEscrowStatus.ESCROWED.getCode());
            escrow.setOutTradeNo(null);
            escrow.setPayBatchId(null);
            bountyEscrowRepository.updateById(escrow);
            sendSystemMessage(task.getId(), "改价补差支付已关闭，可重新发起支付");
            return;
        }

        if ("WAIT_BUYER_PAY".equals(tradeStatus)) {
            escrow.setStatus(BountyEscrowStatus.WAIT_PAY.getCode());
            bountyEscrowRepository.updateById(escrow);
        }
    }

    private BountyPayStatusVO buildStatusVO(BountyPriceChange change, BountyTask task, BountyEscrow escrow) {
        BigDecimal delta = increaseDelta(change);
        BountyPayStatusVO vo = new BountyPayStatusVO();
        vo.setPriceChangeId(change.getId());
        vo.setTaskId(task.getId());
        vo.setPriceChangeStatus(change.getStatus());
        vo.setCurrentTaskAmount(amount(task.getFinalAmount()));
        vo.setTargetAmount(amount(change.getTargetAmount()));
        vo.setNeedPay(delta.compareTo(BigDecimal.ZERO) > 0);
        vo.setPaid(amount(task.getFinalAmount()).compareTo(amount(change.getTargetAmount())) >= 0);
        vo.setOutTradeNo(escrow.getOutTradeNo());
        return vo;
    }

    private String buildAppPayOrderString(BountyTask task, Long priceChangeId, String outTradeNo, BigDecimal amount) {
        try {
            AlipayClient alipayClient = buildClient();
            AlipayTradeAppPayRequest alipayRequest = new AlipayTradeAppPayRequest();

            AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
            model.setOutTradeNo(outTradeNo);
            model.setProductCode(APP_PAY_PRODUCT_CODE);
            model.setTotalAmount(amount(amount).toPlainString());
            model.setSubject("悬赏改价补差-" + task.getTaskSn());
            model.setBody("悬赏任务改价补差支付, priceChangeId=" + priceChangeId);
            model.setTimeoutExpress(resolveTimeoutExpress());
            alipayRequest.setBizModel(model);
            alipayRequest.setNotifyUrl(resolveBountyNotifyUrl());
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

    private BountyPayStatusVO syncByChange(BountyTask task, BountyPriceChange change) {
        BountyEscrow escrow = getEscrowOrThrow(task.getId());

        if (!StringUtils.hasText(escrow.getOutTradeNo())) {
            return buildStatusVO(change, task, escrow);
        }

        AlipayTradeQueryResponse queryResponse = queryAlipayTrade(escrow.getOutTradeNo());
        if (queryResponse == null) {
            return buildStatusVO(change, task, escrow);
        }

        String tradeStatus = queryResponse.getTradeStatus();
        String tradeNo = queryResponse.getTradeNo();
        String totalAmount = queryResponse.getTotalAmount();

        if (StringUtils.hasText(totalAmount)) {
            BigDecimal notifyAmount = amount(parseAmount(totalAmount));
            BigDecimal expectAmount = increaseDelta(change);
            if (notifyAmount.compareTo(expectAmount) != 0) {
                throw new BusinessException("补差支付金额校验失败");
            }
        }

        applyTradeStatus(task, change, escrow, tradeStatus, tradeNo, parseAlipayDate(queryResponse.getSendPayDate()));
        BountyTask latestTask = getTaskOrThrow(task.getId());
        BountyEscrow latestEscrow = getEscrowOrThrow(task.getId());
        return buildStatusVO(change, latestTask, latestEscrow);
    }

    private BountyPriceChange getPayRelatedPriceChange(Long taskId) {
        BountyEscrow escrow = bountyEscrowRepository.selectOne(new LambdaQueryWrapper<BountyEscrow>()
                .eq(BountyEscrow::getTaskId, taskId)
                .eq(BountyEscrow::getIsDelete, 0)
                .last("limit 1"));
        if (escrow != null && escrow.getPayBatchId() != null) {
            BountyPriceChange change = bountyPriceChangeRepository.selectById(escrow.getPayBatchId());
            if (change != null && Objects.equals(change.getIsDelete(), 0)) {
                return change;
            }
        }

        return bountyPriceChangeRepository.selectOne(new LambdaQueryWrapper<BountyPriceChange>()
                .eq(BountyPriceChange::getTaskId, taskId)
                .eq(BountyPriceChange::getStatus, BountyPriceChangeStatus.AGREED.getCode())
                .eq(BountyPriceChange::getIsDelete, 0)
                .orderByDesc(BountyPriceChange::getConfirmTime)
                .orderByDesc(BountyPriceChange::getId)
                .last("limit 1"));
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
            log.error("悬赏支付宝回调验签异常", e);
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
            log.warn("悬赏补差主动查单失败 outTradeNo={}", outTradeNo, ex);
            return null;
        }
    }

    private BountyPriceChange getPriceChangeOrThrow(Long id) {
        BountyPriceChange change = bountyPriceChangeRepository.selectById(id);
        if (change == null || Objects.equals(change.getIsDelete(), 1)) {
            throw new BusinessException("改价记录不存在");
        }
        return change;
    }

    private BountyTask getTaskOrThrow(Long id) {
        BountyTask task = bountyTaskRepository.selectById(id);
        if (task == null || Objects.equals(task.getIsDelete(), 1)) {
            throw new BusinessException("悬赏任务不存在");
        }
        return task;
    }

    private BountyEscrow getEscrowOrThrow(Long taskId) {
        BountyEscrow escrow = bountyEscrowRepository.selectOne(new LambdaQueryWrapper<BountyEscrow>()
                .eq(BountyEscrow::getTaskId, taskId)
                .eq(BountyEscrow::getIsDelete, 0)
                .last("limit 1"));
        if (escrow == null) {
            throw new BusinessException("托管记录不存在");
        }
        return escrow;
    }

    private BigDecimal increaseDelta(BountyPriceChange change) {
        return amount(change.getTargetAmount()).subtract(amount(change.getCurrentAmount()));
    }

    private BigDecimal amount(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal parseAmount(String text) {
        if (!StringUtils.hasText(text)) {
            throw new BusinessException("回调缺少 total_amount");
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException("回调金额格式非法");
        }
    }

    private LocalDateTime parseAlipayTime(String value) {
        if (!StringUtils.hasText(value)) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(value, ALIPAY_TIME_FORMATTER);
        } catch (Exception ex) {
            return LocalDateTime.now();
        }
    }

    private LocalDateTime parseAlipayDate(Date value) {
        if (value == null) {
            return LocalDateTime.now();
        }
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private String buildPriceIncreaseOutTradeNo(Long priceChangeId) {
        String suffix = String.valueOf(priceChangeId == null ? 0L : priceChangeId);
        if (suffix.length() > 8) {
            suffix = suffix.substring(suffix.length() - 8);
        }
        return "BPCH" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase()
                + suffix;
    }

    private String resolveTimeoutExpress() {
        int timeoutMinutes = paymentProperties.getTimeoutMinutes() == null ? 30 : paymentProperties.getTimeoutMinutes();
        return Math.max(timeoutMinutes, 1) + "m";
    }

    private String resolveBountyNotifyUrl() {
        String notifyUrl = paymentProperties.getNotifyUrl();
        if (!StringUtils.hasText(notifyUrl)) {
            throw new BusinessException("支付宝回调地址未配置");
        }
        if (notifyUrl.contains("/api/orders/pay/alipay/notify")) {
            return notifyUrl.replace("/api/orders/pay/alipay/notify", "/api/bounty/pay/alipay/notify");
        }
        return notifyUrl;
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

    private void sendSystemMessage(Long taskId, String content) {
        BountyMessage message = new BountyMessage();
        message.setTaskId(taskId);
        message.setSenderId(0L);
        message.setSenderRole("SYSTEM");
        message.setMessageType(3);
        message.setContent(content);
        bountyMessageRepository.insert(message);

        BountyMessageVO vo = new BountyMessageVO();
        vo.setId(message.getId());
        vo.setTaskId(message.getTaskId());
        vo.setSenderId(message.getSenderId());
        vo.setSenderRole(message.getSenderRole());
        vo.setMessageType(message.getMessageType());
        vo.setContent(message.getContent());
        vo.setAttachments(message.getAttachments());
        vo.setCreateTime(message.getCreateTime());
        bountyWebSocketService.broadcastMessage(taskId, vo);
    }
}
