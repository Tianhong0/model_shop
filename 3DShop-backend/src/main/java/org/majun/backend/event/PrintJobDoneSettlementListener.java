package org.majun.backend.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.service.DesignerSettlementService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 打印完成分润结算监听器
 * 监听打印完成事件，触发设计师分润结算
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PrintJobDoneSettlementListener {

    private final DesignerSettlementService designerSettlementService;

    @Async("printTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PrintJobDoneEvent event) {
        if (event == null || event.getOrderId() == null) {
            return;
        }
        try {
            log.info("收到打印完成事件，开始分润结算: orderId={}", event.getOrderId());
            designerSettlementService.settleOnPrintDone(event.getOrderId());
        } catch (Exception ex) {
            log.error("分润结算失败: orderId={}, error={}", event.getOrderId(), ex.getMessage(), ex);
        }
    }
}
