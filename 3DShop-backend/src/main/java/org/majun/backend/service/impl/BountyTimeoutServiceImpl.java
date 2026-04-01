package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.entity.BountyBid;
import org.majun.backend.entity.BountyDelivery;
import org.majun.backend.entity.BountyEscrow;
import org.majun.backend.entity.BountyPriceChange;
import org.majun.backend.entity.BountyStatusLog;
import org.majun.backend.entity.BountyTask;
import org.majun.backend.enums.BountyEscrowStatus;
import org.majun.backend.enums.BountyPriceChangeStatus;
import org.majun.backend.enums.BountyTaskStatus;
import org.majun.backend.repository.BountyBidRepository;
import org.majun.backend.repository.BountyDeliveryRepository;
import org.majun.backend.repository.BountyEscrowRepository;
import org.majun.backend.repository.BountyPriceChangeRepository;
import org.majun.backend.repository.BountyStatusLogRepository;
import org.majun.backend.repository.BountyTaskRepository;
import org.majun.backend.service.BountyTimeoutService;
import org.majun.backend.service.ConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 悬赏超时处理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BountyTimeoutServiceImpl implements BountyTimeoutService {

    private final BountyEscrowRepository escrowRepository;
    private final BountyTaskRepository taskRepository;
    private final BountyPriceChangeRepository priceChangeRepository;
    private final BountyBidRepository bidRepository;
    private final BountyDeliveryRepository deliveryRepository;
    private final BountyStatusLogRepository statusLogRepository;
    private final BountyFinanceService bountyFinanceService;
    private final ConfigService configService;

    // 默认超时配置
    private static final int DEFAULT_ESCROW_PAY_TIMEOUT_HOURS = 24;
    private static final int DEFAULT_ACCEPTANCE_TIMEOUT_DAYS = 7;
    private static final int DEFAULT_RECRUITING_TIMEOUT_DAYS = 30;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int closeTimeoutEscrowPayments() {
        int timeoutHours = getTimeoutConfig("BOUNTY_ESCROW_PAY_TIMEOUT", DEFAULT_ESCROW_PAY_TIMEOUT_HOURS);
        LocalDateTime threshold = LocalDateTime.now().minusHours(timeoutHours);

        // 查询待支付且超时的托管记录
        List<BountyEscrow> timeoutEscrows = escrowRepository.selectList(
                new LambdaQueryWrapper<BountyEscrow>()
                        .eq(BountyEscrow::getStatus, BountyEscrowStatus.WAIT_PAY.getCode())
                        .eq(BountyEscrow::getIsDelete, 0)
                        .le(BountyEscrow::getCreateTime, threshold)
        );

        int count = 0;
        for (BountyEscrow escrow : timeoutEscrows) {
            try {
                // 更新托管状态为已关闭
                escrow.setStatus(BountyEscrowStatus.CLOSED.getCode());
                escrowRepository.updateById(escrow);

                // 更新任务状态为已关闭
                BountyTask task = taskRepository.selectById(escrow.getTaskId());
                if (task != null && task.getStatus().equals(BountyTaskStatus.WAIT_ESCROW_PAYMENT.getCode())) {
                    Integer beforeStatus = task.getStatus();
                    task.setStatus(BountyTaskStatus.CLOSED.getCode());
                    task.setCloseReason("托管支付超时自动关闭");
                    taskRepository.updateById(task);

                    // 记录状态变更日志
                    appendStatusLog(task.getId(), beforeStatus, task.getStatus(), 0L, "SYSTEM", "托管支付超时自动关闭");
                }
                count++;
                log.info("托管支付超时已关闭: escrowId={}, taskId={}", escrow.getId(), escrow.getTaskId());
            } catch (Exception e) {
                log.error("关闭托管支付失败: escrowId={}", escrow.getId(), e);
            }
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int handleTimeoutPriceChanges() {
        // 查询待确认且已过期的改价申请
        List<BountyPriceChange> timeoutChanges = priceChangeRepository.selectList(
                new LambdaQueryWrapper<BountyPriceChange>()
                        .eq(BountyPriceChange::getStatus, BountyPriceChangeStatus.PENDING.getCode())
                        .eq(BountyPriceChange::getIsDelete, 0)
                        .le(BountyPriceChange::getExpireTime, LocalDateTime.now())
        );

        int count = 0;
        for (BountyPriceChange change : timeoutChanges) {
            try {
                // 改价超时自动设置为失效
                change.setStatus(BountyPriceChangeStatus.EXPIRED.getCode());
                change.setConfirmTime(LocalDateTime.now());
                priceChangeRepository.updateById(change);
                count++;
                log.info("改价协商超时已失效: changeId={}, taskId={}", change.getId(), change.getTaskId());
            } catch (Exception e) {
                log.error("处理改价超时失败: changeId={}", change.getId(), e);
            }
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int handleTimeoutTaskDeadline() {
        // 查询交付中但超过截止时间的任务
        List<BountyTask> timeoutTasks = taskRepository.selectList(
                new LambdaQueryWrapper<BountyTask>()
                        .eq(BountyTask::getStatus, BountyTaskStatus.IN_DELIVERY.getCode())
                        .eq(BountyTask::getIsDelete, 0)
                        .isNotNull(BountyTask::getDeadlineTime)
                        .le(BountyTask::getDeadlineTime, LocalDateTime.now())
        );

        int count = 0;
        for (BountyTask task : timeoutTasks) {
            try {
                // 进入争议状态
                Integer beforeStatus = task.getStatus();
                task.setStatus(BountyTaskStatus.DISPUTED.getCode());
                task.setCloseReason("设计者未按时交付，已进入争议状态");
                taskRepository.updateById(task);

                // 记录状态变更日志
                appendStatusLog(task.getId(), beforeStatus, task.getStatus(), 0L, "SYSTEM", "任务截止时间超时，自动进入争议状态");
                count++;
                log.info("任务截止时间超时进入争议: taskId={}", task.getId());
            } catch (Exception e) {
                log.error("处理任务截止时间超时失败: taskId={}", task.getId(), e);
            }
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int handleTimeoutAcceptance() {
        int timeoutDays = getTimeoutConfig("BOUNTY_ACCEPTANCE_TIMEOUT", DEFAULT_ACCEPTANCE_TIMEOUT_DAYS);
        LocalDateTime threshold = LocalDateTime.now().minusDays(timeoutDays);

        // 查询待验收且超时的任务
        List<BountyTask> timeoutTasks = taskRepository.selectList(
                new LambdaQueryWrapper<BountyTask>()
                        .eq(BountyTask::getStatus, BountyTaskStatus.WAIT_ACCEPTANCE.getCode())
                        .eq(BountyTask::getIsDelete, 0)
                        .isNotNull(BountyTask::getDeliveryTime)
                        .le(BountyTask::getDeliveryTime, threshold)
        );

        int count = 0;
        for (BountyTask task : timeoutTasks) {
            try {
                // 检查是否有已提交的交付记录
                BountyDelivery delivery = deliveryRepository.selectOne(
                        new LambdaQueryWrapper<BountyDelivery>()
                                .eq(BountyDelivery::getTaskId, task.getId())
                                .eq(BountyDelivery::getIsDelete, 0)
                                .orderByDesc(BountyDelivery::getCreateTime)
                                .last("LIMIT 1")
                );

                if (delivery == null) {
                    log.warn("任务待验收但无交付记录，跳过: taskId={}", task.getId());
                    continue;
                }

                // 自动验收通过
                Integer beforeStatus = task.getStatus();
                bountyFinanceService.releaseToWinner(task);
                task.setStatus(BountyTaskStatus.COMPLETED.getCode());
                taskRepository.updateById(task);

                // 更新交付状态为已验收
                delivery.setStatus(3); // ACCEPTED
                deliveryRepository.updateById(delivery);

                // 记录状态变更日志
                appendStatusLog(task.getId(), beforeStatus, task.getStatus(), 0L, "SYSTEM", "验收超时自动通过");
                count++;
                log.info("验收超时自动通过: taskId={}", task.getId());
            } catch (Exception e) {
                log.error("自动验收失败: taskId={}", task.getId(), e);
            }
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int handleTimeoutRecruiting() {
        int timeoutDays = getTimeoutConfig("BOUNTY_RECRUITING_TIMEOUT", DEFAULT_RECRUITING_TIMEOUT_DAYS);
        LocalDateTime threshold = LocalDateTime.now().minusDays(timeoutDays);

        // 查询招募中且超过截止时间或超时的任务
        List<BountyTask> timeoutTasks = taskRepository.selectList(
                new LambdaQueryWrapper<BountyTask>()
                        .eq(BountyTask::getStatus, BountyTaskStatus.RECRUITING.getCode())
                        .eq(BountyTask::getIsDelete, 0)
                        .and(wrapper -> wrapper
                                .isNotNull(BountyTask::getDeadlineTime)
                                .le(BountyTask::getDeadlineTime, LocalDateTime.now())
                                .or()
                                .le(BountyTask::getCreateTime, threshold)
                        )
        );

        int count = 0;
        for (BountyTask task : timeoutTasks) {
            try {
                // 检查是否有竞标
                Long bidCount = bidRepository.selectCount(
                        new LambdaQueryWrapper<BountyBid>()
                                .eq(BountyBid::getTaskId, task.getId())
                                .eq(BountyBid::getIsDelete, 0)
                );

                Integer beforeStatus = task.getStatus();
                if (bidCount == null || bidCount == 0) {
                    // 无竞标，关闭任务并退款
                    bountyFinanceService.refundToPublisher(task);
                    task.setStatus(BountyTaskStatus.CLOSED.getCode());
                    task.setCloseReason("招募超时无人竞标");
                    appendStatusLog(task.getId(), beforeStatus, task.getStatus(), 0L, "SYSTEM", "招募超时无人竞标，自动关闭");
                } else {
                    // 有竞标但未选标，关闭任务并退款
                    bountyFinanceService.refundToPublisher(task);
                    task.setStatus(BountyTaskStatus.CLOSED.getCode());
                    task.setCloseReason("招募超时未选标");
                    appendStatusLog(task.getId(), beforeStatus, task.getStatus(), 0L, "SYSTEM", "招募超时未选标，自动关闭");
                }
                taskRepository.updateById(task);
                count++;
                log.info("招募超时已关闭: taskId={}, bidCount={}", task.getId(), bidCount);
            } catch (Exception e) {
                log.error("处理招募超时失败: taskId={}", task.getId(), e);
            }
        }
        return count;
    }

    /**
     * 从配置获取超时时间
     */
    private int getTimeoutConfig(String key, int defaultValue) {
        try {
            String value = configService.getConfigValue(key);
            if (value != null && !value.isBlank()) {
                return Integer.parseInt(value.trim());
            }
        } catch (NumberFormatException e) {
            log.warn("解析超时配置失败，使用默认值: key={}, defaultValue={}", key, defaultValue);
        }
        return defaultValue;
    }

    /**
     * 记录状态变更日志
     */
    private void appendStatusLog(Long taskId, Integer fromStatus, Integer toStatus, Long operatorId, String operatorRole, String remark) {
        BountyStatusLog statusLog = new BountyStatusLog();
        statusLog.setTaskId(taskId);
        statusLog.setFromStatus(fromStatus);
        statusLog.setToStatus(toStatus);
        statusLog.setOperatorId(operatorId);
        statusLog.setOperatorRole(operatorRole);
        statusLog.setRemark(remark);
        statusLogRepository.insert(statusLog);
    }
}
