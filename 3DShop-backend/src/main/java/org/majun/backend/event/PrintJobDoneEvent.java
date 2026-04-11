package org.majun.backend.event;

import lombok.Getter;

/**
 * 打印任务完成事件
 * 用于触发打印完成后的业务流程（如自动发货）
 */
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
