package org.majun.backend.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.service.OrderDeliveryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 物流轨迹自动推进定时任务
 * 自动模拟物流轨迹更新
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryTrackAutoTask {

    private final OrderDeliveryService orderDeliveryService;

    @Value("${delivery.auto-track-enabled:true}")
    private boolean autoTrackEnabled;

    @Scheduled(fixedDelayString = "${delivery.auto-track-interval-ms:60000}")
    public void autoAdvanceTracks() {
        if (!autoTrackEnabled) {
            return;
        }
        try {
            orderDeliveryService.autoAdvanceTracks();
        } catch (Exception ex) {
            log.warn("自动推进物流轨迹失败", ex);
        }
    }
}
