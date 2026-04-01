package org.majun.backend.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.entity.PrintJob;
import org.majun.backend.entity.SysOrder;
import org.majun.backend.enums.OrderStatus;
import org.majun.backend.enums.PrintJobStatus;
import org.majun.backend.event.PrintJobDoneEvent;
import org.majun.backend.repository.PrintJobRepository;
import org.majun.backend.repository.SysOrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 订单状态同步补偿任务
 * 用于处理打印任务已完成但订单状态未更新的情况
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusSyncTask {

    private final PrintJobRepository printJobRepository;
    private final SysOrderRepository orderRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 每5分钟执行一次，检查是否有打印任务已完成但订单状态仍为"生产中"的情况
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    public void syncOrderStatusAfterPrintDone() {
        try {
            // 查询所有已完成的打印任务
            List<PrintJob> doneJobs = printJobRepository.selectList(
                    new LambdaQueryWrapper<PrintJob>()
                            .eq(PrintJob::getIsDelete, 0)
                            .eq(PrintJob::getStatus, PrintJobStatus.DONE.getCode())
                            .isNotNull(PrintJob::getOrderId)
            );

            if (doneJobs == null || doneJobs.isEmpty()) {
                return;
            }

            int fixedCount = 0;
            for (PrintJob job : doneJobs) {
                if (job.getOrderId() == null) {
                    continue;
                }

                // 检查订单状态是否仍为"生产中"
                SysOrder order = orderRepository.selectById(job.getOrderId());
                if (order == null || !Objects.equals(order.getOrderStatus(), OrderStatus.IN_PRODUCTION.getCode())) {
                    continue;
                }

                // 重新发布打印完成事件，触发自动发货流程
                log.info("检测到订单状态未同步，重新发布打印完成事件: orderId={}, jobId={}", order.getId(), job.getId());
                applicationEventPublisher.publishEvent(new PrintJobDoneEvent(job.getId(), order.getId(), order.getOrderSn()));
                fixedCount++;
            }

            if (fixedCount > 0) {
                log.info("订单状态同步补偿完成，修复了 {} 条记录", fixedCount);
            }
        } catch (Exception ex) {
            log.warn("订单状态同步补偿任务执行失败", ex);
        }
    }
}
