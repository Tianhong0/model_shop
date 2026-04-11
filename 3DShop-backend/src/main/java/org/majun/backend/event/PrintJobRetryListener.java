package org.majun.backend.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.service.PrintJobService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 打印任务重试监听器
 * 监听重试事件，重新执行打印流程
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PrintJobRetryListener {

    private final PrintJobService printJobService;

    @Async("printTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PrintJobRetryEvent event) {
        if (event == null || event.getJobId() == null) {
            return;
        }
        try {
            printJobService.runPipeline(event.getJobId(), event.getPrinterId());
        } catch (Exception ex) {
            log.error("重试打印任务执行失败 jobId={}", event.getJobId(), ex);
        }
    }
}
