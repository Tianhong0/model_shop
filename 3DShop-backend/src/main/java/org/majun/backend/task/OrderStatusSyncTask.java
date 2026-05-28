package org.majun.backend.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.entity.DesignerSettlement;
import org.majun.backend.entity.PrintJob;
import org.majun.backend.entity.SysOrder;
import org.majun.backend.entity.SysOrderDelivery;
import org.majun.backend.enums.OrderStatus;
import org.majun.backend.enums.PrintJobStatus;
import org.majun.backend.repository.DesignerSettlementRepository;
import org.majun.backend.repository.PrintJobRepository;
import org.majun.backend.repository.SysOrderDeliveryRepository;
import org.majun.backend.repository.SysOrderRepository;
import org.majun.backend.service.DesignerSettlementService;
import org.majun.backend.service.OrderDeliveryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 订单状态同步与分润补偿任务
 * 兜底链路：不依赖事件机制，直接调用 service，保证以下两点：
 *   1) 打印完成但订单仍卡在"生产中" -> 调用 autoShipByOrderId 推进发货
 *   2) 打印完成但未生成分润记录 -> 调用 settleOnPrintDone 补单
 * 每个 service 调用都是独立事务，互相不影响。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusSyncTask {

    private final PrintJobRepository printJobRepository;
    private final SysOrderRepository orderRepository;
    private final SysOrderDeliveryRepository deliveryRepository;
    private final DesignerSettlementRepository designerSettlementRepository;
    private final DesignerSettlementService designerSettlementService;
    private final OrderDeliveryService orderDeliveryService;

    @Value("${delivery.auto-ship-enabled:true}")
    private boolean autoShipEnabled;

    /**
     * 每5分钟执行一次
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    public void syncOrderStatusAfterPrintDone() {
        try {
            List<PrintJob> doneJobs = printJobRepository.selectList(
                    new LambdaQueryWrapper<PrintJob>()
                            .eq(PrintJob::getIsDelete, 0)
                            .eq(PrintJob::getStatus, PrintJobStatus.DONE.getCode())
                            .isNotNull(PrintJob::getOrderId)
            );

            if (doneJobs == null || doneJobs.isEmpty()) {
                return;
            }

            int reshippedCount = 0;
            int resettledCount = 0;
            for (PrintJob job : doneJobs) {
                if (job.getOrderId() == null) {
                    continue;
                }
                SysOrder order = orderRepository.selectById(job.getOrderId());
                if (order == null) {
                    continue;
                }

                // 1) 订单仍卡在"生产中"，且未生成物流单 -> 直接补自动发货
                if (autoShipEnabled
                        && Objects.equals(order.getOrderStatus(), OrderStatus.IN_PRODUCTION.getCode())) {
                    Long existedDelivery = deliveryRepository.selectCount(
                            new LambdaQueryWrapper<SysOrderDelivery>()
                                    .eq(SysOrderDelivery::getOrderId, order.getId())
                                    .eq(SysOrderDelivery::getIsDelete, 0)
                    );
                    if (existedDelivery == null || existedDelivery == 0) {
                        try {
                            log.info("检测到订单卡在生产中，补发自动发货: orderId={}, jobId={}", order.getId(), job.getId());
                            Long deliveryId = orderDeliveryService.autoShipByOrderId(order.getId(), job.getId());
                            if (deliveryId != null) {
                                reshippedCount++;
                            }
                        } catch (Exception ex) {
                            log.error("补发自动发货失败: orderId={}, error={}", order.getId(), ex.getMessage(), ex);
                        }
                    }
                }

                // 2) 检查分润记录是否缺失（独立判断，与发货状态解耦）
                Long settled = designerSettlementRepository.selectCount(
                        new LambdaQueryWrapper<DesignerSettlement>()
                                .eq(DesignerSettlement::getOrderId, order.getId())
                );
                if (settled == null || settled == 0) {
                    try {
                        log.info("检测到分润记录缺失，补发分润结算: orderId={}, jobId={}", order.getId(), job.getId());
                        designerSettlementService.settleOnPrintDone(order.getId());
                        // settleOnPrintDone 本身可能因为"非设计师模型"等条件提前 return，
                        // 这里只统计真正新增了记录的情况
                        Long after = designerSettlementRepository.selectCount(
                                new LambdaQueryWrapper<DesignerSettlement>()
                                        .eq(DesignerSettlement::getOrderId, order.getId())
                        );
                        if (after != null && after > 0) {
                            resettledCount++;
                        }
                    } catch (Exception ex) {
                        log.error("补发分润结算失败: orderId={}, error={}", order.getId(), ex.getMessage(), ex);
                    }
                }
            }

            if (reshippedCount > 0 || resettledCount > 0) {
                log.info("订单状态同步补偿完成，补发货 {} 条，补分润 {} 条", reshippedCount, resettledCount);
            }
        } catch (Exception ex) {
            log.warn("订单状态同步补偿任务执行失败", ex);
        }
    }
}
