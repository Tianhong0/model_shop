package org.majun.backend.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.service.CustomerServiceService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 客服会话超时定时任务
 * 定时关闭长时间无响应的客服会话
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CsConversationTimeoutTask {

    private final CustomerServiceService customerServiceService;

    /**
     * 每分钟检查一次超时会话
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void closeExpiredConversations() {
        try {
            customerServiceService.closeExpiredConversations();
        } catch (Exception ex) {
            log.error("定时关闭超时会话失败", ex);
        }
    }
}
