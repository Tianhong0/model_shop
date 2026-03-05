package org.majun.backend.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.service.OrderPaymentService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaymentCloseTask {

    private final OrderPaymentService orderPaymentService;

    @Scheduled(cron = "0 */1 * * * ?")
    public void closeTimeoutPayments() {
        try {
            orderPaymentService.closeTimeoutPayments();
        } catch (Exception ex) {
            log.error("定时关闭超时订单失败", ex);
        }
    }
}
