package org.majun.backend.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.entity.PrintJobEvent;
import org.majun.backend.repository.PrintJobEventRepository;
import org.majun.backend.service.OrderDeliveryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PrintJobDoneDeliveryListener {

    private final OrderDeliveryService orderDeliveryService;
    private final PrintJobEventRepository printJobEventRepository;

    @Value("${delivery.auto-ship-enabled:true}")
    private boolean autoShipEnabled;

    @Async("printTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PrintJobDoneEvent event) {
        if (event == null || event.getOrderId() == null || event.getJobId() == null) {
            return;
        }
        if (!autoShipEnabled) {
            saveJobEvent(event.getJobId(), "AUTO_SHIP_SKIPPED", "自动发货已禁用", null);
            return;
        }

        try {
            Long deliveryId = orderDeliveryService.autoShipByOrderId(event.getOrderId(), event.getJobId());
            if (deliveryId != null) {
                saveJobEvent(event.getJobId(), "AUTO_SHIPPED", "打印完成自动发货成功", String.valueOf(deliveryId));
            } else {
                saveJobEvent(event.getJobId(), "AUTO_SHIP_SKIPPED", "订单已存在物流单，自动发货跳过", null);
            }
        } catch (Exception ex) {
            saveJobEvent(event.getJobId(), "AUTO_SHIP_FAILED", "打印完成自动发货失败", ex.getMessage());
            log.error("打印完成自动发货失败, orderId={}, jobId={}", event.getOrderId(), event.getJobId(), ex);
        }
    }

    private void saveJobEvent(Long jobId, String type, String message, String payload) {
        PrintJobEvent event = new PrintJobEvent();
        event.setJobId(jobId);
        event.setEventType(type);
        event.setEventMessage(message);
        event.setEventPayload(payload);
        printJobEventRepository.insert(event);
    }
}
