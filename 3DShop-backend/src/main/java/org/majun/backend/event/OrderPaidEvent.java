package org.majun.backend.event;

import lombok.Getter;

/**
 * 订单支付完成事件
 * 用于触发支付后业务流程（如自动创建打印任务）
 */
@Getter
public class OrderPaidEvent {

    private final Long orderId;

    public OrderPaidEvent(Long orderId) {
        this.orderId = orderId;
    }
}
