package org.majun.backend.event;

import lombok.Getter;

@Getter
public class PrintJobDoneEvent {

    private final Long jobId;
    private final Long orderId;
    private final String orderSn;

    public PrintJobDoneEvent(Long jobId, Long orderId, String orderSn) {
        this.jobId = jobId;
        this.orderId = orderId;
        this.orderSn = orderSn;
    }
}
