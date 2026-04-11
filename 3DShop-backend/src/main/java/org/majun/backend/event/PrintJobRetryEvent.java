package org.majun.backend.event;

import lombok.Getter;

/**
 * 打印任务重试事件
 * 用于触发打印任务的重试流程
 */
@Getter
public class PrintJobRetryEvent {

    private final Long jobId;
    private final Long printerId;

    public PrintJobRetryEvent(Long jobId, Long printerId) {
        this.jobId = jobId;
        this.printerId = printerId;
    }
}
