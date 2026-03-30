package org.majun.backend.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.service.impl.WalletService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 钱包冻结资金定时解冻任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletFrozenSchedule {

    private final WalletService walletService;

    /**
     * 每小时执行一次，解冻到期资金
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void unfreezeExpiredRecords() {
        try {
            int count = walletService.unfreezeExpiredRecords();
            if (count > 0) {
                log.info("解冻到期资金记录数: {}", count);
            }
        } catch (Exception e) {
            log.error("解冻到期资金任务执行失败", e);
        }
    }
}
