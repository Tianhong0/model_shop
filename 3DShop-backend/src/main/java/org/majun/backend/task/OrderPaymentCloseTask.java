package org.majun.backend.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.service.OrderPaymentService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单支付超时关闭定时任务
 * 每分钟检查并关闭超时未支付的订单
 */
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
