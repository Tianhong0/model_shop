package org.majun.backend.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.service.BountyTimeoutService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 悬赏超时处理定时任务
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BountyTimeoutTask {

    private final BountyTimeoutService bountyTimeoutService;

    /**
     * 每分钟检查托管支付超时
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void closeTimeoutEscrowPayments() {
        try {
            int count = bountyTimeoutService.closeTimeoutEscrowPayments();
            if (count > 0) {
                log.info("关闭超时托管支付记录数: {}", count);
            }
        } catch (Exception e) {
            log.error("关闭超时托管支付任务失败", e);
        }
    }

    /**
     * 每小时检查改价协商超时
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void handleTimeoutPriceChanges() {
        try {
            int count = bountyTimeoutService.handleTimeoutPriceChanges();
            if (count > 0) {
                log.info("处理超时改价协商记录数: {}", count);
            }
        } catch (Exception e) {
            log.error("处理改价协商超时任务失败", e);
        }
    }

    /**
     * 每小时检查任务截止时间超时
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void handleTimeoutTaskDeadline() {
        try {
            int count = bountyTimeoutService.handleTimeoutTaskDeadline();
            if (count > 0) {
                log.info("处理任务截止时间超时记录数: {}", count);
            }
        } catch (Exception e) {
            log.error("处理任务截止时间超时任务失败", e);
        }
    }

    /**
     * 每小时检查验收超时
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void handleTimeoutAcceptance() {
        try {
            int count = bountyTimeoutService.handleTimeoutAcceptance();
            if (count > 0) {
                log.info("自动验收超时任务数: {}", count);
            }
        } catch (Exception e) {
            log.error("处理验收超时任务失败", e);
        }
    }

    /**
     * 每小时检查招募超时
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void handleTimeoutRecruiting() {
        try {
            int count = bountyTimeoutService.handleTimeoutRecruiting();
            if (count > 0) {
                log.info("处理招募超时任务数: {}", count);
            }
        } catch (Exception e) {
            log.error("处理招募超时任务失败", e);
        }
    }
}
