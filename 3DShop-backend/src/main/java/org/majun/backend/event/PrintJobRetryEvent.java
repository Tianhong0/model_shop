package org.majun.backend.event;

import lombok.Getter;

@Getter
public class PrintJobRetryEvent {

    private final Long jobId;
    private final Long printerId;

    public PrintJobRetryEvent(Long jobId, Long printerId) {
        this.jobId = jobId;
        this.printerId = printerId;
    }
}
