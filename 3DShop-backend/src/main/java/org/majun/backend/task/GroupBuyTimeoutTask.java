package org.majun.backend.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.service.GroupBuyService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 拼团超时定时任务
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroupBuyTimeoutTask {

    private final GroupBuyService groupBuyService;

    /**
     * 处理超时拼团 - 每5分钟执行
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void processTimeoutGroups() {
        try {
            groupBuyService.processTimeoutGroups();
        } catch (Exception ex) {
            log.error("处理拼团超时失败", ex);
        }
    }

    /**
     * 更新活动状态 - 每小时执行
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void processActivityStatus() {
        try {
            groupBuyService.processActivityStatus();
        } catch (Exception ex) {
            log.error("更新活动状态失败", ex);
        }
    }
}
