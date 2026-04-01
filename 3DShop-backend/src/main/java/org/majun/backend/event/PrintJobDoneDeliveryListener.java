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
        log.info("收到打印完成事件: jobId={}, orderId={}, orderSn={}",
                event != null ? event.getJobId() : null,
                event != null ? event.getOrderId() : null,
                event != null ? event.getOrderSn() : null);

        if (event == null || event.getOrderId() == null || event.getJobId() == null) {
            log.warn("打印完成事件参数无效，跳过处理: event={}", event);
            return;
        }
        if (!autoShipEnabled) {
            log.info("自动发货已禁用，跳过处理: orderId={}", event.getOrderId());
            saveJobEvent(event.getJobId(), "AUTO_SHIP_SKIPPED", "自动发货已禁用", null);
            return;
        }

        try {
            log.info("开始执行自动发货: orderId={}, jobId={}", event.getOrderId(), event.getJobId());
            Long deliveryId = orderDeliveryService.autoShipByOrderId(event.getOrderId(), event.getJobId());
            if (deliveryId != null) {
                log.info("自动发货成功: orderId={}, jobId={}, deliveryId={}", event.getOrderId(), event.getJobId(), deliveryId);
                saveJobEvent(event.getJobId(), "AUTO_SHIPPED", "打印完成自动发货成功", String.valueOf(deliveryId));
            } else {
                log.info("自动发货跳过（订单已存在物流单）: orderId={}, jobId={}", event.getOrderId(), event.getJobId());
                saveJobEvent(event.getJobId(), "AUTO_SHIP_SKIPPED", "订单已存在物流单，自动发货跳过", null);
            }
        } catch (Exception ex) {
            log.error("打印完成自动发货失败, orderId={}, jobId={}, error={}", event.getOrderId(), event.getJobId(), ex.getMessage(), ex);
            saveJobEvent(event.getJobId(), "AUTO_SHIP_FAILED", "打印完成自动发货失败", ex.getMessage());
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
