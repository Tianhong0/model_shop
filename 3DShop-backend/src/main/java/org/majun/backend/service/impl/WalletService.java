package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.CertAlipayRequest;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayFundTransToaccountTransferModel;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayFundTransToaccountTransferRequest;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayFundTransToaccountTransferResponse;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.config.PaymentProperties;
import org.majun.backend.dto.WalletLedgerQueryRequest;
import org.majun.backend.dto.WalletRechargeRequest;
import org.majun.backend.dto.WalletWithdrawApplyRequest;
import org.majun.backend.dto.WalletWithdrawAuditRequest;
import org.majun.backend.dto.WalletWithdrawPayRequest;
import org.majun.backend.dto.WalletWithdrawQueryRequest;
import org.majun.backend.entity.WalletAccount;
import org.majun.backend.entity.WalletFrozenRecord;
import org.majun.backend.entity.WalletLedger;
import org.majun.backend.entity.WalletWithdraw;
import org.majun.backend.enums.WalletLedgerDirection;
import org.majun.backend.enums.WalletWithdrawStatus;
import org.majun.backend.repository.WalletAccountRepository;
import org.majun.backend.repository.WalletFrozenRecordRepository;
import org.majun.backend.repository.WalletLedgerRepository;
import org.majun.backend.repository.WalletWithdrawRepository;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.WalletAccountVO;
import org.majun.backend.vo.WalletFrozenDetailVO;
import org.majun.backend.vo.WalletLedgerVO;
import org.majun.backend.vo.WalletRechargePayCreateResponse;
import org.majun.backend.vo.WalletRechargeStatusVO;
import org.majun.backend.vo.WalletWithdrawVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WalletService {

    private static final String APP_PAY_PRODUCT_CODE = "QUICK_MSECURITY_PAY";
    private static final String BIZ_TYPE_RECHARGE = "WALLET_RECHARGE";
    private static final DateTimeFormatter ALIPAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final BigDecimal MIN_WITHDRAW = new BigDecimal("1.00");
    private static final BigDecimal MIN_RECHARGE = new BigDecimal("0.01");

    private final WalletAccountRepository walletAccountRepository;
    private final WalletLedgerRepository walletLedgerRepository;
    private final WalletWithdrawRepository walletWithdrawRepository;
    private final WalletFrozenRecordRepository walletFrozenRecordRepository;
    private final PaymentProperties paymentProperties;
    private final ObjectMapper objectMapper;

    private static final int USED_ORDER_FROZEN_DAYS = 7;

    public WalletAccountVO getAccount(Long userId) {
        WalletAccount account = getOrCreateWallet(userId);
        WalletAccountVO vo = new WalletAccountVO();
        vo.setUserId(String.valueOf(account.getUserId()));
        vo.setAvailableBalance(amount(account.getAvailableBalance()));
        vo.setFrozenBalance(amount(account.getFrozenBalance()));
        vo.setStatus(account.getStatus());
        return vo;
    }

    public PageResult<WalletLedgerVO> pageLedger(WalletLedgerQueryRequest request, Long userId) {
        LambdaQueryWrapper<WalletLedger> wrapper = new LambdaQueryWrapper<WalletLedger>()
                .eq(WalletLedger::getUserId, userId)
                .orderByDesc(WalletLedger::getCreateTime);
        if (request.getDirection() != null) {
            wrapper.eq(WalletLedger::getDirection, request.getDirection());
        }
        if (request.getBizType() != null && !request.getBizType().isBlank()) {
            wrapper.eq(WalletLedger::getBizType, request.getBizType().trim());
        }

        Page<WalletLedger> page = new Page<>(request.getPageNum(), request.getPageSize());
        walletLedgerRepository.selectPage(page, wrapper);

        List<WalletLedgerVO> records = page.getRecords().stream().map(this::toLedgerVO).toList();
        return PageResult.<WalletLedgerVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    public PageResult<WalletWithdrawVO> pageMyWithdraw(WalletWithdrawQueryRequest request, Long userId) {
        return pageWithdraw(request, userId, false);
    }

    public PageResult<WalletWithdrawVO> pageAdminWithdraw(WalletWithdrawQueryRequest request) {
        return pageWithdraw(request, null, true);
    }

    public WalletRechargePayCreateResponse createRechargePayOrder(WalletRechargeRequest request, Long userId) {
        validateAlipayConfig();
        BigDecimal amount = amount(request.getAmount());
        if (amount.compareTo(MIN_RECHARGE) < 0) {
            throw new BusinessException("充值金额必须大于0");
        }

        WalletAccount account = getOrCreateWallet(userId);
        if (!Objects.equals(account.getStatus(), 1)) {
            throw new BusinessException("钱包状态异常，无法充值");
        }

        String outTradeNo = generateRechargeOutTradeNo(userId);
        String orderString = buildRechargeAppPayOrderString(outTradeNo, amount);

        WalletRechargePayCreateResponse response = new WalletRechargePayCreateResponse();
        response.setOutTradeNo(outTradeNo);
        response.setAmount(amount);
        response.setOrderString(orderString);
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public WalletRechargeStatusVO syncRechargeStatus(String outTradeNo, Long userId) {
        if (!StringUtils.hasText(outTradeNo)) {
            throw new BusinessException("充值单号不能为空");
        }
        Long ownerId = parseUserIdFromRechargeOutTradeNo(outTradeNo);
        if (!Objects.equals(ownerId, userId)) {
            throw new BusinessException("无权查询该充值单");
        }

        WalletLedger existing = walletLedgerRepository.selectOne(new LambdaQueryWrapper<WalletLedger>()
                .eq(WalletLedger::getBizType, BIZ_TYPE_RECHARGE)
                .eq(WalletLedger::getBizNo, outTradeNo)
                .last("limit 1"));
        if (existing != null) {
            WalletRechargeStatusVO vo = new WalletRechargeStatusVO();
            vo.setOutTradeNo(outTradeNo);
            vo.setStatus(1);
            vo.setAmount(amount(existing.getAmount()));
            vo.setTradeNo(readTradeNoFromRemark(existing.getRemark()));
            vo.setPayTime(existing.getCreateTime());
            return vo;
        }

        AlipayTradeQueryResponse queryResponse = queryAlipayTrade(outTradeNo);
        WalletRechargeStatusVO vo = new WalletRechargeStatusVO();
        vo.setOutTradeNo(outTradeNo);
        if (queryResponse == null) {
            vo.setStatus(0);
            return vo;
        }

        String tradeStatus = queryResponse.getTradeStatus();
        BigDecimal tradeAmount = parseAmount(queryResponse.getTotalAmount());
        String tradeNo = queryResponse.getTradeNo();
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            processRechargeSuccess(userId, outTradeNo, tradeAmount, tradeNo, parseAlipayDate(queryResponse.getSendPayDate()), null);
            vo.setStatus(1);
            vo.setAmount(tradeAmount);
            vo.setTradeNo(tradeNo);
            vo.setPayTime(parseAlipayDate(queryResponse.getSendPayDate()));
            return vo;
        }
        if ("TRADE_CLOSED".equals(tradeStatus)) {
            vo.setStatus(2);
            vo.setAmount(tradeAmount);
            vo.setTradeNo(tradeNo);
            return vo;
        }
        vo.setStatus(0);
        vo.setAmount(tradeAmount);
        vo.setTradeNo(tradeNo);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean handleRechargeAlipayNotify(HttpServletRequest request) {
        Map<String, String> notifyParams = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                notifyParams.put(key, values[0]);
            }
        });
        if (notifyParams.isEmpty()) {
            return false;
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
            throw new BusinessException("回调参数不完整");
        }
        if (!("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus))) {
            return true;
        }

        Long userId = parseUserIdFromRechargeOutTradeNo(outTradeNo);
        BigDecimal totalAmount = parseAmount(notifyParams.get("total_amount"));
        String tradeNo = notifyParams.get("trade_no");
        LocalDateTime payTime = parseAlipayTime(notifyParams.get("gmt_payment"));
        processRechargeSuccess(userId, outTradeNo, totalAmount, tradeNo, payTime, toJson(notifyParams));
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public String applyWithdraw(WalletWithdrawApplyRequest request, Long userId) {
        BigDecimal amount = amount(request.getAmount());
        if (amount.compareTo(MIN_WITHDRAW) < 0) {
            throw new BusinessException("提现金额不能低于1元");
        }

        WalletAccount account = getOrCreateWallet(userId);
        if (!Objects.equals(account.getStatus(), 1)) {
            throw new BusinessException("钱包状态异常，无法提现");
        }

        BigDecimal beforeAvailable = amount(account.getAvailableBalance());
        BigDecimal beforeFrozen = amount(account.getFrozenBalance());
        if (beforeAvailable.compareTo(amount) < 0) {
            throw new BusinessException("钱包余额不足");
        }

        WalletWithdraw withdraw = new WalletWithdraw();
        withdraw.setUserId(userId);
        withdraw.setWithdrawSn(generateWithdrawSn());
        withdraw.setAmount(amount);
        withdraw.setStatus(WalletWithdrawStatus.APPLIED.getCode());
        withdraw.setApplyRemark(trimText(request.getRemark(), 200));
        withdraw.setAlipayAccount(trimText(request.getAlipayAccount(), 128));
        withdraw.setAlipayRealName(trimText(request.getAlipayRealName(), 64));
        withdraw.setIsDelete(0);
        walletWithdrawRepository.insert(withdraw);

        BigDecimal afterAvailable = beforeAvailable.subtract(amount);
        BigDecimal afterFrozen = beforeFrozen.add(amount);
        account.setAvailableBalance(afterAvailable);
        account.setFrozenBalance(afterFrozen);
        walletAccountRepository.updateById(account);

        WalletLedger ledger = new WalletLedger();
        ledger.setUserId(userId);
        ledger.setAccountId(account.getId());
        ledger.setDirection(WalletLedgerDirection.FREEZE.getCode());
        ledger.setBizType("WITHDRAW_FREEZE");
        ledger.setBizNo(String.valueOf(withdraw.getId()));
        ledger.setRefId(withdraw.getId());
        ledger.setAmount(amount);
        ledger.setBeforeAvailable(beforeAvailable);
        ledger.setAfterAvailable(afterAvailable);
        ledger.setBeforeFrozen(beforeFrozen);
        ledger.setAfterFrozen(afterFrozen);
        ledger.setRemark("提现申请冻结金额");
        walletLedgerRepository.insert(ledger);
        LocalDateTime now = LocalDateTime.now();
        withdraw.setStatus(WalletWithdrawStatus.APPROVED.getCode());
        withdraw.setAuditRemark("系统自动审核通过");
        withdraw.setAuditBy(0L);
        withdraw.setAuditTime(now);
        walletWithdrawRepository.updateById(withdraw);

        try {
            String transferTradeNo = executeAlipayTransfer(withdraw.getWithdrawSn(), amount, withdraw.getAlipayAccount(), withdraw.getAlipayRealName());
            BigDecimal payAfterFrozen = afterFrozen.subtract(amount);
            if (payAfterFrozen.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("提现冻结余额异常");
            }
            account.setFrozenBalance(payAfterFrozen);
            walletAccountRepository.updateById(account);

            WalletLedger payLedger = new WalletLedger();
            payLedger.setUserId(userId);
            payLedger.setAccountId(account.getId());
            payLedger.setDirection(WalletLedgerDirection.EXPENSE.getCode());
            payLedger.setBizType("WITHDRAW_PAY");
            payLedger.setBizNo(String.valueOf(withdraw.getId()));
            payLedger.setRefId(withdraw.getId());
            payLedger.setAmount(amount);
            payLedger.setBeforeAvailable(afterAvailable);
            payLedger.setAfterAvailable(afterAvailable);
            payLedger.setBeforeFrozen(afterFrozen);
            payLedger.setAfterFrozen(payAfterFrozen);
            payLedger.setRemark("提现自动打款成功，支付宝转账号:" + transferTradeNo);
            walletLedgerRepository.insert(payLedger);

            withdraw.setStatus(WalletWithdrawStatus.PAID.getCode());
            withdraw.setPayRemark("系统自动打款成功，支付宝转账号:" + transferTradeNo);
            withdraw.setPayBy(0L);
            withdraw.setPayTime(LocalDateTime.now());
            walletWithdrawRepository.updateById(withdraw);
        } catch (Exception ex) {
            BigDecimal rollbackAvailable = afterAvailable.add(amount);
            BigDecimal rollbackFrozen = afterFrozen.subtract(amount);
            if (rollbackFrozen.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("提现冻结余额异常");
            }
            account.setAvailableBalance(rollbackAvailable);
            account.setFrozenBalance(rollbackFrozen);
            walletAccountRepository.updateById(account);

            WalletLedger failLedger = new WalletLedger();
            failLedger.setUserId(userId);
            failLedger.setAccountId(account.getId());
            failLedger.setDirection(WalletLedgerDirection.UNFREEZE.getCode());
            failLedger.setBizType("WITHDRAW_PAY_FAIL_UNFREEZE");
            failLedger.setBizNo(String.valueOf(withdraw.getId()));
            failLedger.setRefId(withdraw.getId());
            failLedger.setAmount(amount);
            failLedger.setBeforeAvailable(afterAvailable);
            failLedger.setAfterAvailable(rollbackAvailable);
            failLedger.setBeforeFrozen(afterFrozen);
            failLedger.setAfterFrozen(rollbackFrozen);
            failLedger.setRemark("自动提现打款失败，金额已退回");
            walletLedgerRepository.insert(failLedger);

            withdraw.setStatus(WalletWithdrawStatus.PAY_FAILED.getCode());
            withdraw.setPayRemark("系统自动打款失败：" + trimText(ex.getMessage(), 250));
            withdraw.setPayBy(0L);
            withdraw.setPayTime(LocalDateTime.now());
            walletWithdrawRepository.updateById(withdraw);
        }

        return String.valueOf(withdraw.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void auditWithdraw(WalletWithdrawAuditRequest request, Long adminId) {
        WalletWithdraw withdraw = getWithdrawOrThrow(request.getWithdrawId());
        if (!Objects.equals(withdraw.getStatus(), WalletWithdrawStatus.APPLIED.getCode())) {
            throw new BusinessException("当前状态不可审核");
        }
        if (Objects.equals(request.getDecision(), 1)) {
            withdraw.setStatus(WalletWithdrawStatus.APPROVED.getCode());
            withdraw.setAuditRemark(trimText(request.getRemark(), 300));
            withdraw.setAuditBy(adminId);
            withdraw.setAuditTime(LocalDateTime.now());
            walletWithdrawRepository.updateById(withdraw);
            return;
        }
        if (!Objects.equals(request.getDecision(), 2)) {
            throw new BusinessException("审核结果无效");
        }

        WalletAccount account = getOrCreateWallet(withdraw.getUserId());
        BigDecimal amount = amount(withdraw.getAmount());
        BigDecimal beforeAvailable = amount(account.getAvailableBalance());
        BigDecimal beforeFrozen = amount(account.getFrozenBalance());
        BigDecimal afterAvailable = beforeAvailable.add(amount);
        BigDecimal afterFrozen = beforeFrozen.subtract(amount);
        if (afterFrozen.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("提现冻结余额异常");
        }

        account.setAvailableBalance(afterAvailable);
        account.setFrozenBalance(afterFrozen);
        walletAccountRepository.updateById(account);

        WalletLedger ledger = new WalletLedger();
        ledger.setUserId(withdraw.getUserId());
        ledger.setAccountId(account.getId());
        ledger.setDirection(WalletLedgerDirection.UNFREEZE.getCode());
        ledger.setBizType("WITHDRAW_REJECT_UNFREEZE");
        ledger.setBizNo(String.valueOf(withdraw.getId()));
        ledger.setRefId(withdraw.getId());
        ledger.setAmount(amount);
        ledger.setBeforeAvailable(beforeAvailable);
        ledger.setAfterAvailable(afterAvailable);
        ledger.setBeforeFrozen(beforeFrozen);
        ledger.setAfterFrozen(afterFrozen);
        ledger.setRemark("提现审核拒绝，解冻余额");
        walletLedgerRepository.insert(ledger);

        withdraw.setStatus(WalletWithdrawStatus.REJECTED.getCode());
        withdraw.setAuditRemark(trimText(request.getRemark(), 300));
        withdraw.setAuditBy(adminId);
        withdraw.setAuditTime(LocalDateTime.now());
        walletWithdrawRepository.updateById(withdraw);
    }

    @Transactional(rollbackFor = Exception.class)
    public void payWithdraw(WalletWithdrawPayRequest request, Long adminId) {
        WalletWithdraw withdraw = getWithdrawOrThrow(request.getWithdrawId());
        if (!Objects.equals(withdraw.getStatus(), WalletWithdrawStatus.APPROVED.getCode())) {
            throw new BusinessException("当前状态不可打款");
        }

        WalletAccount account = getOrCreateWallet(withdraw.getUserId());
        BigDecimal amount = amount(withdraw.getAmount());
        BigDecimal beforeAvailable = amount(account.getAvailableBalance());
        BigDecimal beforeFrozen = amount(account.getFrozenBalance());

        if (Objects.equals(request.getPayResult(), 1)) {
            String transferTradeNo = executeAlipayTransfer(withdraw.getWithdrawSn(), amount, withdraw.getAlipayAccount(), withdraw.getAlipayRealName());
            BigDecimal afterFrozen = beforeFrozen.subtract(amount);
            if (afterFrozen.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("提现冻结余额异常");
            }
            account.setFrozenBalance(afterFrozen);
            walletAccountRepository.updateById(account);

            WalletLedger ledger = new WalletLedger();
            ledger.setUserId(withdraw.getUserId());
            ledger.setAccountId(account.getId());
            ledger.setDirection(WalletLedgerDirection.EXPENSE.getCode());
            ledger.setBizType("WITHDRAW_PAY");
            ledger.setBizNo(String.valueOf(withdraw.getId()));
            ledger.setRefId(withdraw.getId());
            ledger.setAmount(amount);
            ledger.setBeforeAvailable(beforeAvailable);
            ledger.setAfterAvailable(beforeAvailable);
            ledger.setBeforeFrozen(beforeFrozen);
            ledger.setAfterFrozen(afterFrozen);
                ledger.setRemark("提现打款成功，支付宝转账号:" + transferTradeNo);
            walletLedgerRepository.insert(ledger);

            withdraw.setStatus(WalletWithdrawStatus.PAID.getCode());
                String manualRemark = trimText(request.getRemark(), 300);
                withdraw.setPayRemark(StringUtils.hasText(manualRemark)
                    ? manualRemark + "；支付宝转账号:" + transferTradeNo
                    : "支付宝转账成功，转账号:" + transferTradeNo);
            withdraw.setPayBy(adminId);
            withdraw.setPayTime(LocalDateTime.now());
            walletWithdrawRepository.updateById(withdraw);
            return;
        }

        if (!Objects.equals(request.getPayResult(), 2)) {
            throw new BusinessException("打款结果无效");
        }

        BigDecimal afterAvailable = beforeAvailable.add(amount);
        BigDecimal afterFrozen = beforeFrozen.subtract(amount);
        if (afterFrozen.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("提现冻结余额异常");
        }
        account.setAvailableBalance(afterAvailable);
        account.setFrozenBalance(afterFrozen);
        walletAccountRepository.updateById(account);

        WalletLedger ledger = new WalletLedger();
        ledger.setUserId(withdraw.getUserId());
        ledger.setAccountId(account.getId());
        ledger.setDirection(WalletLedgerDirection.UNFREEZE.getCode());
        ledger.setBizType("WITHDRAW_PAY_FAIL_UNFREEZE");
        ledger.setBizNo(String.valueOf(withdraw.getId()));
        ledger.setRefId(withdraw.getId());
        ledger.setAmount(amount);
        ledger.setBeforeAvailable(beforeAvailable);
        ledger.setAfterAvailable(afterAvailable);
        ledger.setBeforeFrozen(beforeFrozen);
        ledger.setAfterFrozen(afterFrozen);
        ledger.setRemark("提现打款失败，余额解冻");
        walletLedgerRepository.insert(ledger);

        withdraw.setStatus(WalletWithdrawStatus.PAY_FAILED.getCode());
        withdraw.setPayRemark(trimText(request.getRemark(), 300));
        withdraw.setPayBy(adminId);
        withdraw.setPayTime(LocalDateTime.now());
        walletWithdrawRepository.updateById(withdraw);
    }

    private PageResult<WalletWithdrawVO> pageWithdraw(WalletWithdrawQueryRequest request, Long userId, boolean adminMode) {
        LambdaQueryWrapper<WalletWithdraw> wrapper = new LambdaQueryWrapper<WalletWithdraw>()
                .eq(WalletWithdraw::getIsDelete, 0)
                .orderByDesc(WalletWithdraw::getCreateTime);
        if (!adminMode) {
            wrapper.eq(WalletWithdraw::getUserId, userId);
        } else if (request.getUserId() != null) {
            wrapper.eq(WalletWithdraw::getUserId, request.getUserId());
        }
        if (request.getStatus() != null) {
            wrapper.eq(WalletWithdraw::getStatus, request.getStatus());
        }

        Page<WalletWithdraw> page = new Page<>(request.getPageNum(), request.getPageSize());
        walletWithdrawRepository.selectPage(page, wrapper);
        List<WalletWithdrawVO> records = page.getRecords().stream().map(this::toWithdrawVO).toList();

        return PageResult.<WalletWithdrawVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    private WalletWithdraw getWithdrawOrThrow(Long withdrawId) {
        WalletWithdraw withdraw = walletWithdrawRepository.selectById(withdrawId);
        if (withdraw == null || Objects.equals(withdraw.getIsDelete(), 1)) {
            throw new BusinessException("提现记录不存在");
        }
        return withdraw;
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

    private WalletLedgerVO toLedgerVO(WalletLedger entity) {
        WalletLedgerVO vo = new WalletLedgerVO();
        vo.setId(String.valueOf(entity.getId()));
        vo.setDirection(entity.getDirection());
        vo.setBizType(entity.getBizType());
        vo.setBizNo(entity.getBizNo());
        vo.setRefId(entity.getRefId() == null ? null : String.valueOf(entity.getRefId()));
        vo.setAmount(amount(entity.getAmount()));
        vo.setBeforeAvailable(amount(entity.getBeforeAvailable()));
        vo.setAfterAvailable(amount(entity.getAfterAvailable()));
        vo.setBeforeFrozen(amount(entity.getBeforeFrozen()));
        vo.setAfterFrozen(amount(entity.getAfterFrozen()));
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private WalletWithdrawVO toWithdrawVO(WalletWithdraw entity) {
        WalletWithdrawVO vo = new WalletWithdrawVO();
        vo.setId(String.valueOf(entity.getId()));
        vo.setUserId(String.valueOf(entity.getUserId()));
        vo.setWithdrawSn(entity.getWithdrawSn());
        vo.setAmount(amount(entity.getAmount()));
        vo.setStatus(entity.getStatus());
        vo.setApplyRemark(entity.getApplyRemark());
        vo.setAlipayAccount(entity.getAlipayAccount());
        vo.setAlipayRealName(entity.getAlipayRealName());
        vo.setAuditRemark(entity.getAuditRemark());
        vo.setPayRemark(entity.getPayRemark());
        vo.setAuditBy(entity.getAuditBy() == null ? null : String.valueOf(entity.getAuditBy()));
        vo.setAuditTime(entity.getAuditTime());
        vo.setPayBy(entity.getPayBy() == null ? null : String.valueOf(entity.getPayBy()));
        vo.setPayTime(entity.getPayTime());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private BigDecimal amount(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String trimText(String text, int maxLen) {
        if (text == null) return null;
        String value = text.trim();
        if (value.length() <= maxLen) return value;
        return value.substring(0, maxLen);
    }

    private void processRechargeSuccess(Long userId,
                                        String outTradeNo,
                                        BigDecimal amount,
                                        String tradeNo,
                                        LocalDateTime payTime,
                                        String notifyContent) {
        WalletLedger exists = walletLedgerRepository.selectOne(new LambdaQueryWrapper<WalletLedger>()
                .eq(WalletLedger::getBizType, BIZ_TYPE_RECHARGE)
                .eq(WalletLedger::getBizNo, outTradeNo)
                .last("limit 1"));
        if (exists != null) {
            return;
        }

        WalletAccount account = getOrCreateWallet(userId);
        if (!Objects.equals(account.getStatus(), 1)) {
            throw new BusinessException("钱包状态异常，无法入账");
        }
        BigDecimal beforeAvailable = amount(account.getAvailableBalance());
        BigDecimal beforeFrozen = amount(account.getFrozenBalance());
        BigDecimal afterAvailable = beforeAvailable.add(amount(amount));

        WalletLedger ledger = new WalletLedger();
        ledger.setUserId(userId);
        ledger.setAccountId(account.getId());
        ledger.setDirection(WalletLedgerDirection.INCOME.getCode());
        ledger.setBizType(BIZ_TYPE_RECHARGE);
        ledger.setBizNo(outTradeNo);
        ledger.setRefId(account.getId());
        ledger.setAmount(amount(amount));
        ledger.setBeforeAvailable(beforeAvailable);
        ledger.setAfterAvailable(afterAvailable);
        ledger.setBeforeFrozen(beforeFrozen);
        ledger.setAfterFrozen(beforeFrozen);
        String remark = "支付宝充值成功" + (StringUtils.hasText(tradeNo) ? "，交易号:" + tradeNo : "");
        if (StringUtils.hasText(notifyContent)) {
            remark = trimText(remark + "；" + notifyContent, 500);
        }
        ledger.setRemark(trimText(remark, 500));
        try {
            walletLedgerRepository.insert(ledger);
        } catch (DuplicateKeyException ex) {
            return;
        }

        account.setAvailableBalance(afterAvailable);
        walletAccountRepository.updateById(account);
    }

    private String buildRechargeAppPayOrderString(String outTradeNo, BigDecimal amount) {
        try {
            AlipayClient alipayClient = buildClient();
            AlipayTradeAppPayRequest alipayRequest = new AlipayTradeAppPayRequest();

            AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
            model.setOutTradeNo(outTradeNo);
            model.setProductCode(APP_PAY_PRODUCT_CODE);
            model.setTotalAmount(amount(amount).toPlainString());
            model.setSubject("3DShop钱包充值");
            model.setBody("3DShop 用户钱包余额充值");
            model.setTimeoutExpress(resolveTimeoutExpress());
            alipayRequest.setBizModel(model);
            alipayRequest.setNotifyUrl(resolveWalletNotifyUrl());

            AlipayTradeAppPayResponse response = alipayClient.sdkExecute(alipayRequest);
            if (response == null || !response.isSuccess() || !StringUtils.hasText(response.getBody())) {
                String subMsg = response == null ? "null" : response.getSubMsg();
                throw new BusinessException("调用支付宝充值下单失败: " + subMsg);
            }
            return response.getBody();
        } catch (AlipayApiException e) {
            throw new BusinessException("调用支付宝充值下单异常", e);
        }
    }

    private String executeAlipayTransfer(String outBizNo, BigDecimal amount, String alipayAccount, String realName) {
        if (!StringUtils.hasText(alipayAccount)) {
            throw new BusinessException("提现支付宝账号不能为空");
        }
        validateAlipayTransferConfig();
        try {
            AlipayFundTransToaccountTransferRequest request = new AlipayFundTransToaccountTransferRequest();
            AlipayFundTransToaccountTransferModel model = new AlipayFundTransToaccountTransferModel();
            model.setOutBizNo(outBizNo);
            model.setPayeeType("ALIPAY_LOGONID");
            model.setPayeeAccount(alipayAccount.trim());
            model.setAmount(amount(amount).toPlainString());
            model.setRemark("3DShop钱包提现");
            if (StringUtils.hasText(realName)) {
                model.setPayeeRealName(realName.trim());
            }
            request.setBizModel(model);
            AlipayFundTransToaccountTransferResponse response = buildClient().certificateExecute(request);
            if (response == null || !response.isSuccess()) {
                String subMsg = response == null ? "null" : response.getSubMsg();
                throw new BusinessException("支付宝转账失败: " + subMsg);
            }
            return StringUtils.hasText(response.getOrderId()) ? response.getOrderId() : outBizNo;
        } catch (AlipayApiException ex) {
            throw new BusinessException("调用支付宝转账异常", ex);
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

    private boolean verifyNotifySign(Map<String, String> params) {
        try {
            return AlipaySignature.rsaCertCheckV1(
                    params,
                    resolveResourcePath(paymentProperties.getAlipayPublicCertPath()),
                    paymentProperties.getCharset(),
                    paymentProperties.getSignType()
            );
        } catch (AlipayApiException e) {
            return false;
        }
    }

    private String resolveTimeoutExpress() {
        int minutes = paymentProperties.getTimeoutMinutes() == null ? 30 : Math.max(paymentProperties.getTimeoutMinutes(), 1);
        return minutes + "m";
    }

    private String resolveWalletNotifyUrl() {
        String notifyUrl = paymentProperties.getNotifyUrl();
        if (!StringUtils.hasText(notifyUrl)) {
            throw new BusinessException("未配置支付宝回调地址");
        }
        if (notifyUrl.contains("/api/orders/pay/alipay/notify")) {
            return notifyUrl.replace("/api/orders/pay/alipay/notify", "/api/wallet/recharge/alipay/notify");
        }
        return notifyUrl;
    }

    private String resolveResourcePath(String path) {
        if (!StringUtils.hasText(path)) {
            throw new BusinessException("支付宝证书路径未配置");
        }
        if (path.startsWith("classpath:")) {
            String location = path.substring("classpath:".length());
            String normalized = location.startsWith("/") ? location.substring(1) : location;
            try {
                var resource = new org.springframework.core.io.ClassPathResource(normalized);
                return resource.getFile().getAbsolutePath();
            } catch (Exception ex) {
                throw new BusinessException("无法读取支付宝证书文件: " + path, ex);
            }
        }
        return path;
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

    private void validateAlipayTransferConfig() {
        if (!StringUtils.hasText(paymentProperties.getAppId())
                || !StringUtils.hasText(paymentProperties.getPrivateKey())
                || !StringUtils.hasText(paymentProperties.getAppCertPath())
                || !StringUtils.hasText(paymentProperties.getAlipayPublicCertPath())
                || !StringUtils.hasText(paymentProperties.getAlipayRootCertPath())
                || !StringUtils.hasText(paymentProperties.getGatewayUrl())) {
            throw new BusinessException("支付宝转账配置不完整，请检查 payment.alipay 配置");
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
        return LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault());
    }

    private BigDecimal parseAmount(String totalAmount) {
        if (!StringUtils.hasText(totalAmount)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        try {
            return amount(new BigDecimal(totalAmount.trim()));
        } catch (Exception ex) {
            throw new BusinessException("金额格式非法");
        }
    }

    private Long parseUserIdFromRechargeOutTradeNo(String outTradeNo) {
        if (!StringUtils.hasText(outTradeNo) || !outTradeNo.startsWith("WRC")) {
            throw new BusinessException("充值单号非法");
        }
        int idx = outTradeNo.lastIndexOf('U');
        if (idx < 0 || idx >= outTradeNo.length() - 1) {
            throw new BusinessException("充值单号非法");
        }
        try {
            return Long.parseLong(outTradeNo.substring(idx + 1));
        } catch (NumberFormatException ex) {
            throw new BusinessException("充值单号非法");
        }
    }

    private String readTradeNoFromRemark(String remark) {
        if (!StringUtils.hasText(remark)) {
            return null;
        }
        int idx = remark.indexOf("交易号:");
        if (idx < 0) {
            return null;
        }
        String val = remark.substring(idx + 4);
        int end = val.indexOf('；');
        if (end >= 0) {
            return val.substring(0, end);
        }
        return val;
    }

    private String toJson(Map<String, String> data) {
        try {
            Map<String, String> safeData = data == null ? Collections.emptyMap() : data;
            return objectMapper.writeValueAsString(safeData);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String generateWithdrawSn() {
        return "WD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private String generateRechargeOutTradeNo(Long userId) {
        return "WRC"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + (int) (Math.random() * 900 + 100)
                + "U"
                + userId;
    }

    // ==================== 冻结资金相关方法 ====================

    /**
     * 冻结资金（二手交易卖家收入）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long freezeAmount(Long userId, BigDecimal amount, String bizType, String bizNo, Long refId, Integer frozenDays, String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        WalletAccount account = getOrCreateWallet(userId);
        if (!Objects.equals(account.getStatus(), 1)) {
            throw new BusinessException("钱包状态异常，无法冻结");
        }

        BigDecimal beforeAvailable = amount(account.getAvailableBalance());
        BigDecimal beforeFrozen = amount(account.getFrozenBalance());
        BigDecimal afterFrozen = beforeFrozen.add(amount);

        // 更新冻结余额
        int updated = walletAccountRepository.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<WalletAccount>()
                        .eq(WalletAccount::getId, account.getId())
                        .eq(WalletAccount::getVersion, account.getVersion())
                        .eq(WalletAccount::getIsDelete, 0)
                        .set(WalletAccount::getFrozenBalance, afterFrozen)
                        .set(WalletAccount::getVersion, account.getVersion() + 1)
        );
        if (updated <= 0) {
            throw new BusinessException("冻结余额失败，请重试");
        }

        // 记录流水
        WalletLedger ledger = new WalletLedger();
        ledger.setUserId(userId);
        ledger.setAccountId(account.getId());
        ledger.setDirection(WalletLedgerDirection.FREEZE.getCode());
        ledger.setBizType(bizType);
        ledger.setBizNo(bizNo);
        ledger.setRefId(refId);
        ledger.setAmount(amount);
        ledger.setBeforeAvailable(beforeAvailable);
        ledger.setAfterAvailable(beforeAvailable);
        ledger.setBeforeFrozen(beforeFrozen);
        ledger.setAfterFrozen(afterFrozen);
        ledger.setRemark(remark);
        walletLedgerRepository.insert(ledger);

        // 创建冻结记录
        LocalDateTime now = LocalDateTime.now();
        int days = frozenDays != null && frozenDays > 0 ? frozenDays : USED_ORDER_FROZEN_DAYS;
        WalletFrozenRecord frozenRecord = new WalletFrozenRecord();
        frozenRecord.setUserId(userId);
        frozenRecord.setAccountId(account.getId());
        frozenRecord.setAmount(amount);
        frozenRecord.setBizType(bizType);
        frozenRecord.setBizNo(bizNo);
        frozenRecord.setRefId(refId);
        frozenRecord.setFrozenDays(days);
        frozenRecord.setFrozenStartTime(now);
        frozenRecord.setFrozenEndTime(now.plusDays(days));
        frozenRecord.setStatus(0);
        frozenRecord.setRemark(remark);
        frozenRecord.setIsDelete(0);
        walletFrozenRecordRepository.insert(frozenRecord);

        return frozenRecord.getId();
    }

    /**
     * 解冻到期资金
     */
    @Transactional(rollbackFor = Exception.class)
    public int unfreezeExpiredRecords() {
        LocalDateTime now = LocalDateTime.now();
        List<WalletFrozenRecord> expiredRecords = walletFrozenRecordRepository.selectList(
                new LambdaQueryWrapper<WalletFrozenRecord>()
                        .eq(WalletFrozenRecord::getStatus, 0)
                        .le(WalletFrozenRecord::getFrozenEndTime, now)
                        .eq(WalletFrozenRecord::getIsDelete, 0)
        );

        int count = 0;
        for (WalletFrozenRecord record : expiredRecords) {
            try {
                unfreezeRecord(record);
                count++;
            } catch (Exception e) {
                // 记录错误但继续处理其他记录
            }
        }
        return count;
    }

    /**
     * 解冻单条记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void unfreezeRecord(WalletFrozenRecord record) {
        if (record == null || !Objects.equals(record.getStatus(), 0)) {
            return;
        }

        WalletAccount account = walletAccountRepository.selectById(record.getAccountId());
        if (account == null || Objects.equals(account.getIsDelete(), 1)) {
            throw new BusinessException("钱包账户不存在");
        }

        BigDecimal beforeAvailable = amount(account.getAvailableBalance());
        BigDecimal beforeFrozen = amount(account.getFrozenBalance());
        BigDecimal afterAvailable = beforeAvailable.add(record.getAmount());
        BigDecimal afterFrozen = beforeFrozen.subtract(record.getAmount());

        if (afterFrozen.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("冻结余额不足");
        }

        // 更新账户余额
        int updated = walletAccountRepository.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<WalletAccount>()
                        .eq(WalletAccount::getId, account.getId())
                        .eq(WalletAccount::getVersion, account.getVersion())
                        .eq(WalletAccount::getIsDelete, 0)
                        .set(WalletAccount::getAvailableBalance, afterAvailable)
                        .set(WalletAccount::getFrozenBalance, afterFrozen)
                        .set(WalletAccount::getVersion, account.getVersion() + 1)
        );
        if (updated <= 0) {
            throw new BusinessException("解冻失败，请重试");
        }

        // 记录流水
        WalletLedger ledger = new WalletLedger();
        ledger.setUserId(record.getUserId());
        ledger.setAccountId(account.getId());
        ledger.setDirection(WalletLedgerDirection.UNFREEZE.getCode());
        ledger.setBizType(record.getBizType() + "_UNFREEZE");
        ledger.setBizNo(record.getBizNo());
        ledger.setRefId(record.getRefId());
        ledger.setAmount(record.getAmount());
        ledger.setBeforeAvailable(beforeAvailable);
        ledger.setAfterAvailable(afterAvailable);
        ledger.setBeforeFrozen(beforeFrozen);
        ledger.setAfterFrozen(afterFrozen);
        ledger.setRemark("冻结资金到期自动解冻");
        walletLedgerRepository.insert(ledger);

        // 更新冻结记录状态
        record.setStatus(1);
        record.setUnfreezeTime(LocalDateTime.now());
        walletFrozenRecordRepository.updateById(record);
    }

    /**
     * 获取用户冻结记录列表
     */
    public List<WalletFrozenDetailVO> listFrozenRecords(Long userId) {
        List<WalletFrozenRecord> records = walletFrozenRecordRepository.selectList(
                new LambdaQueryWrapper<WalletFrozenRecord>()
                        .eq(WalletFrozenRecord::getUserId, userId)
                        .eq(WalletFrozenRecord::getStatus, 0)
                        .eq(WalletFrozenRecord::getIsDelete, 0)
                        .orderByAsc(WalletFrozenRecord::getFrozenEndTime)
        );

        LocalDateTime now = LocalDateTime.now();
        return records.stream().map(record -> {
            WalletFrozenDetailVO vo = new WalletFrozenDetailVO();
            vo.setId(String.valueOf(record.getId()));
            vo.setAmount(amount(record.getAmount()));
            vo.setBizType(record.getBizType());
            vo.setBizNo(record.getBizNo());
            vo.setFrozenDays(record.getFrozenDays());
            vo.setFrozenStartTime(record.getFrozenStartTime());
            vo.setFrozenEndTime(record.getFrozenEndTime());
            vo.setStatus(record.getStatus());
            vo.setRemark(record.getRemark());
            vo.setCreateTime(record.getCreateTime());

            // 计算剩余时间
            if (record.getFrozenEndTime() != null) {
                long remainingSeconds = java.time.Duration.between(now, record.getFrozenEndTime()).getSeconds();
                if (remainingSeconds > 0) {
                    vo.setRemainingDays(remainingSeconds / (24 * 3600));
                    vo.setRemainingHours((remainingSeconds % (24 * 3600)) / 3600);
                } else {
                    vo.setRemainingDays(0L);
                    vo.setRemainingHours(0L);
                }
            }

            return vo;
        }).toList();
    }

    /**
     * 获取用户冻结总额
     */
    public BigDecimal getFrozenAmount(Long userId) {
        List<WalletFrozenRecord> records = walletFrozenRecordRepository.selectList(
                new LambdaQueryWrapper<WalletFrozenRecord>()
                        .eq(WalletFrozenRecord::getUserId, userId)
                        .eq(WalletFrozenRecord::getStatus, 0)
                        .eq(WalletFrozenRecord::getIsDelete, 0)
        );
        return records.stream()
                .map(WalletFrozenRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
