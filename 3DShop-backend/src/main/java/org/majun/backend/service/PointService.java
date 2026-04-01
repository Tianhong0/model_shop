package org.majun.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.dto.PointLedgerQueryRequest;
import org.majun.backend.entity.PointAccount;
import org.majun.backend.entity.PointLedger;
import org.majun.backend.enums.PointLedgerDirection;
import org.majun.backend.repository.PointAccountRepository;
import org.majun.backend.repository.PointLedgerRepository;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.PointAccountVO;
import org.majun.backend.vo.PointLedgerVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    public static final String BIZ_ORDER_PAY = "ORDER_PAY";
    public static final String BIZ_USED_ORDER_SELL = "USED_ORDER_SELL";
    public static final String BIZ_ORDER_POINT_DEDUCT = "ORDER_POINT_DEDUCT";
    public static final String BIZ_ORDER_POINT_REFUND = "ORDER_POINT_REFUND";
    public static final String BIZ_BOUNTY_RELEASE = "BOUNTY_RELEASE";
    public static final String BIZ_ECO_MATERIAL = "ECO_MATERIAL";
    public static final String BIZ_COUPON_EXCHANGE = "COUPON_EXCHANGE";
    public static final String BIZ_COUPON_REFUND = "COUPON_REFUND";
    public static final String BIZ_POST_REPLY_ADOPTED = "POST_REPLY_ADOPTED";
    public static final String BIZ_POST_REPLY_EXCELLENT = "POST_REPLY_EXCELLENT";

    // 配置键名
    public static final String CONFIG_ECO_MATERIAL_REWARD_POINTS = "ECO_MATERIAL_REWARD_POINTS";
    public static final String CONFIG_ECO_MATERIAL_ACTIVITY_MULTIPLIER = "ECO_MATERIAL_ACTIVITY_MULTIPLIER";
    public static final String CONFIG_POST_REPLY_ADOPTED_POINTS = "POST_REPLY_ADOPTED_POINTS";
    public static final String CONFIG_POST_REPLY_EXCELLENT_POINTS = "POST_REPLY_EXCELLENT_POINTS";

    // 默认值
    private static final int DEFAULT_ORDER_REWARD_RATE = 1;
    private static final int DEFAULT_BOUNTY_REWARD_RATE = 1;
    private static final int DEFAULT_ECO_MATERIAL_REWARD_POINTS = 5;
    private static final int DEFAULT_POST_REPLY_POINTS = 3;

    private final PointAccountRepository pointAccountRepository;
    private final PointLedgerRepository pointLedgerRepository;
    private final ConfigService configService;

    public PointAccountVO getAccount(Long userId) {
        PointAccount account = getOrCreateAccount(userId);
        PointAccountVO vo = new PointAccountVO();
        vo.setUserId(String.valueOf(account.getUserId()));
        vo.setAvailablePoints(safePoint(account.getAvailablePoints()));
        vo.setTotalEarned(safePoint(account.getTotalEarned()));
        vo.setTotalSpent(safePoint(account.getTotalSpent()));
        vo.setStatus(account.getStatus());
        return vo;
    }

    public PageResult<PointLedgerVO> pageLedger(PointLedgerQueryRequest request, Long userId) {
        LambdaQueryWrapper<PointLedger> wrapper = new LambdaQueryWrapper<PointLedger>()
                .eq(PointLedger::getUserId, userId)
                .orderByDesc(PointLedger::getCreateTime);
        if (request.getDirection() != null) {
            wrapper.eq(PointLedger::getDirection, request.getDirection());
        }
        if (request.getBizType() != null && !request.getBizType().isBlank()) {
            wrapper.eq(PointLedger::getBizType, request.getBizType().trim());
        }

        Page<PointLedger> page = new Page<>(request.getPageNum(), request.getPageSize());
        pointLedgerRepository.selectPage(page, wrapper);

        List<PointLedgerVO> records = page.getRecords().stream().map(this::toLedgerVO).toList();
        return PageResult.<PointLedgerVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void rewardOrderPaid(Long userId, Long orderId, String orderSn, java.math.BigDecimal payAmount) {
        int points = toRewardPoints(payAmount, DEFAULT_ORDER_REWARD_RATE);
        if (points <= 0) return;
        String bizNo = orderSn;
        increase(userId, points, BIZ_ORDER_PAY, bizNo, orderId, "订单支付奖励积分");
    }

    @Transactional(rollbackFor = Exception.class)
    public void rewardBountyRelease(Long userId, Long taskId, String taskSn, java.math.BigDecimal amount) {
        int points = toRewardPoints(amount, DEFAULT_BOUNTY_REWARD_RATE);
        if (points <= 0) return;
        String bizNo = taskSn;
        increase(userId, points, BIZ_BOUNTY_RELEASE, bizNo, taskId, "悬赏验收结算奖励积分");
    }

    @Transactional(rollbackFor = Exception.class)
    public void rewardUsedOrderSell(Long userId, Long orderId, String orderSn, java.math.BigDecimal amount) {
        int points = toRewardPoints(amount, DEFAULT_ORDER_REWARD_RATE);
        if (points <= 0) return;
        increase(userId, points, BIZ_USED_ORDER_SELL, orderSn, orderId, "二手交易卖出奖励积分");
    }

    @Transactional(rollbackFor = Exception.class)
    public void rewardEcoMaterial(Long userId, Long orderId, String orderSn) {
        int basePoints = getEcoMaterialRewardPoints();
        BigDecimal multiplier = getEcoMaterialActivityMultiplier();
        int finalPoints = BigDecimal.valueOf(basePoints)
                .multiply(multiplier)
                .setScale(0, RoundingMode.DOWN)
                .intValue();

        if (finalPoints <= 0) return;

        String bizNo = "ECO_" + orderSn;
        String remark = "选择环保材料奖励积分";
        if (multiplier.compareTo(BigDecimal.ONE) > 0) {
            remark += "(活动加成x" + multiplier + ")";
        }
        increase(userId, finalPoints, BIZ_ECO_MATERIAL, bizNo, orderId, remark);
    }

    /**
     * 获取环保材料奖励积分（从配置读取）
     */
    private int getEcoMaterialRewardPoints() {
        String value = configService.getConfigValue(CONFIG_ECO_MATERIAL_REWARD_POINTS, String.valueOf(DEFAULT_ECO_MATERIAL_REWARD_POINTS));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("解析环保材料奖励积分配置失败，使用默认值: {}", DEFAULT_ECO_MATERIAL_REWARD_POINTS);
            return DEFAULT_ECO_MATERIAL_REWARD_POINTS;
        }
    }

    /**
     * 获取环保材料活动加成倍率
     */
    private BigDecimal getEcoMaterialActivityMultiplier() {
        String value = configService.getConfigValue(CONFIG_ECO_MATERIAL_ACTIVITY_MULTIPLIER, "1.0");
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return BigDecimal.ONE;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void rewardReplyAdopted(Long userId, Long replyId, Long postId) {
        int points = getReplyAdoptedPoints();
        if (points <= 0) return;
        String bizNo = "ADOPTED_" + replyId;
        increase(userId, points, BIZ_POST_REPLY_ADOPTED, bizNo, postId, "社区回答被采纳奖励积分");
    }

    @Transactional(rollbackFor = Exception.class)
    public void rewardReplyExcellent(Long userId, Long replyId, Long postId) {
        int points = getReplyExcellentPoints();
        if (points <= 0) return;
        String bizNo = "EXCELLENT_" + replyId;
        increase(userId, points, BIZ_POST_REPLY_EXCELLENT, bizNo, postId, "社区优质回答奖励积分");
    }

    /**
     * 获取回答被采纳奖励积分（从配置读取）
     */
    private int getReplyAdoptedPoints() {
        String value = configService.getConfigValue(CONFIG_POST_REPLY_ADOPTED_POINTS, String.valueOf(DEFAULT_POST_REPLY_POINTS));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return DEFAULT_POST_REPLY_POINTS;
        }
    }

    /**
     * 获取优质回答奖励积分（从配置读取）
     */
    private int getReplyExcellentPoints() {
        String value = configService.getConfigValue(CONFIG_POST_REPLY_EXCELLENT_POINTS, String.valueOf(DEFAULT_POST_REPLY_POINTS));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return DEFAULT_POST_REPLY_POINTS;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void consumeOrderPoints(Long userId, Long orderId, String orderSn, Integer points) {
        int value = safePoint(points);
        if (value <= 0) return;
        decrease(userId, value, BIZ_ORDER_POINT_DEDUCT, orderSn, orderId, "订单积分抵扣");
    }

    @Transactional(rollbackFor = Exception.class)
    public void refundOrderPoints(Long userId, Long orderId, String orderSn, Integer points) {
        int value = safePoint(points);
        if (value <= 0) return;
        increase(userId, value, BIZ_ORDER_POINT_REFUND, orderSn, orderId, "订单取消返还积分");
    }

    @Transactional(rollbackFor = Exception.class)
    public void increase(Long userId, int points, String bizType, String bizNo, Long refId, String remark) {
        if (points <= 0) return;
        if (existsLedger(bizType, bizNo)) return;
        PointAccount account = getOrCreateAccount(userId);
        int before = safePoint(account.getAvailablePoints());
        int after = before + points;

        PointLedger ledger = new PointLedger();
        ledger.setUserId(userId);
        ledger.setAccountId(account.getId());
        ledger.setDirection(PointLedgerDirection.INCOME.getCode());
        ledger.setBizType(bizType);
        ledger.setBizNo(bizNo);
        ledger.setRefId(refId);
        ledger.setPoints(points);
        ledger.setBeforePoints(before);
        ledger.setAfterPoints(after);
        ledger.setRemark(remark);
        pointLedgerRepository.insert(ledger);

        account.setAvailablePoints(after);
        account.setTotalEarned(safePoint(account.getTotalEarned()) + points);
        pointAccountRepository.updateById(account);
    }

    @Transactional(rollbackFor = Exception.class)
    public void decrease(Long userId, int points, String bizType, String bizNo, Long refId, String remark) {
        if (points <= 0) return;
        if (existsLedger(bizType, bizNo)) return;
        PointAccount account = getOrCreateAccount(userId);
        int before = safePoint(account.getAvailablePoints());
        if (before < points) {
            throw new BusinessException("积分不足");
        }
        int after = before - points;

        PointLedger ledger = new PointLedger();
        ledger.setUserId(userId);
        ledger.setAccountId(account.getId());
        ledger.setDirection(PointLedgerDirection.EXPENSE.getCode());
        ledger.setBizType(bizType);
        ledger.setBizNo(bizNo);
        ledger.setRefId(refId);
        ledger.setPoints(points);
        ledger.setBeforePoints(before);
        ledger.setAfterPoints(after);
        ledger.setRemark(remark);
        pointLedgerRepository.insert(ledger);

        account.setAvailablePoints(after);
        account.setTotalSpent(safePoint(account.getTotalSpent()) + points);
        pointAccountRepository.updateById(account);
    }

    private PointAccount getOrCreateAccount(Long userId) {
        PointAccount account = pointAccountRepository.selectOne(new LambdaQueryWrapper<PointAccount>()
                .eq(PointAccount::getUserId, userId)
                .eq(PointAccount::getIsDelete, 0)
                .last("limit 1"));
        if (account != null) {
            return account;
        }
        PointAccount created = new PointAccount();
        created.setUserId(userId);
        created.setAvailablePoints(0);
        created.setTotalEarned(0);
        created.setTotalSpent(0);
        created.setStatus(1);
        created.setIsDelete(0);
        pointAccountRepository.insert(created);
        return created;
    }

    private boolean existsLedger(String bizType, String bizNo) {
        if (bizType == null || bizNo == null) return false;
        Long count = pointLedgerRepository.selectCount(new LambdaQueryWrapper<PointLedger>()
                .eq(PointLedger::getBizType, bizType)
                .eq(PointLedger::getBizNo, bizNo));
        return count != null && count > 0;
    }

    private PointLedgerVO toLedgerVO(PointLedger entity) {
        PointLedgerVO vo = new PointLedgerVO();
        vo.setId(String.valueOf(entity.getId()));
        vo.setDirection(entity.getDirection());
        vo.setBizType(entity.getBizType());
        vo.setBizNo(entity.getBizNo());
        vo.setRefId(entity.getRefId() == null ? null : String.valueOf(entity.getRefId()));
        vo.setPoints(safePoint(entity.getPoints()));
        vo.setBeforePoints(safePoint(entity.getBeforePoints()));
        vo.setAfterPoints(safePoint(entity.getAfterPoints()));
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private int safePoint(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private int toRewardPoints(java.math.BigDecimal amount, int rate) {
        java.math.BigDecimal safe = amount == null ? java.math.BigDecimal.ZERO : amount;
        if (safe.compareTo(java.math.BigDecimal.ZERO) <= 0) return 0;
        return safe.multiply(java.math.BigDecimal.valueOf(rate)).setScale(0, java.math.RoundingMode.DOWN).intValue();
    }

    public String generateBizNoByTimePrefix(String prefix) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        return prefix + now;
    }
}
