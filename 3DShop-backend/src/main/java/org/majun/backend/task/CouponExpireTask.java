package org.majun.backend.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.entity.UserCoupon;
import org.majun.backend.enums.UserCouponStatus;
import org.majun.backend.repository.UserCouponRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 优惠券过期定时任务
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CouponExpireTask {

    private final UserCouponRepository userCouponRepository;

    /**
     * 每小时执行一次，处理过期优惠券
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void expireCoupons() {
        try {
            LambdaUpdateWrapper<UserCoupon> updateWrapper = new LambdaUpdateWrapper<UserCoupon>()
                    .eq(UserCoupon::getStatus, UserCouponStatus.UNUSED.getCode())
                    .lt(UserCoupon::getEndTime, LocalDateTime.now())
                    .set(UserCoupon::getStatus, UserCouponStatus.EXPIRED.getCode());

            int count = userCouponRepository.update(null, updateWrapper);
            if (count > 0) {
                log.info("已处理 {} 张过期优惠券", count);
            }
        } catch (Exception ex) {
            log.error("优惠券过期处理失败", ex);
        }
    }
}
