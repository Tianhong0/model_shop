package org.majun.backend.service;

/**
 * 悬赏超时处理服务接口
 */
public interface BountyTimeoutService {

    /**
     * 关闭超时托管支付
     * 处理 WAIT_ESCROW_PAYMENT 状态超过配置时间的任务
     * @return 处理的记录数
     */
    int closeTimeoutEscrowPayments();

    /**
     * 处理改价协商超时
     * 处理超过 expireTime 的待确认改价申请
     * @return 处理的记录数
     */
    int handleTimeoutPriceChanges();

    /**
     * 处理任务截止时间超时
     * 处理 IN_DELIVERY 状态超过 deadlineTime 的任务
     * @return 处理的记录数
     */
    int handleTimeoutTaskDeadline();

    /**
     * 处理验收超时
     * 处理 WAIT_ACCEPTANCE 状态超过配置天数的任务，自动验收通过
     * @return 处理的记录数
     */
    int handleTimeoutAcceptance();

    /**
     * 处理招募超时
     * 处理 RECRUITING 状态超过截止时间且无竞标或未选标的任务
     * @return 处理的记录数
     */
    int handleTimeoutRecruiting();
}
