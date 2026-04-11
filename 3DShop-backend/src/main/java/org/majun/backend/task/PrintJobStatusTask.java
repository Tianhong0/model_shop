package org.majun.backend.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.config.OctoPrintProperties;
import org.majun.backend.service.PrintJobService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 打印任务状态轮询定时任务
 * 定期从打印机获取任务状态并同步到数据库
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PrintJobStatusTask {

    private final PrintJobService printJobService;
    private final OctoPrintProperties octoPrintProperties;

    @Scheduled(fixedDelayString = "#{@octoPrintProperties.pollIntervalMs}")
    public void syncStatus() {
        try {
            if (octoPrintProperties.getPollIntervalMs() == null || octoPrintProperties.getPollIntervalMs() <= 0) {
                return;
            }
            printJobService.syncAndBroadcastRunningJobs();
        } catch (Exception ex) {
            log.warn("轮询打印任务状态失败", ex);
        }
    }
}
