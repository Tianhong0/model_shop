package org.majun.backend.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.service.PrintJobService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 订单支付完成监听器
 * 监听订单支付事件，自动触发打印任务创建
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaidPrintListener {

    private final PrintJobService printJobService;

    @Async("printTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderPaidEvent event) {
        if (event == null || event.getOrderId() == null) {
            return;
        }
        try {
            printJobService.createAndDispatchFromPaidOrder(event.getOrderId());
        } catch (Exception ex) {
            log.error("支付后自动切片打印触发失败 orderId={}", event.getOrderId(), ex);
        }
    }
}
