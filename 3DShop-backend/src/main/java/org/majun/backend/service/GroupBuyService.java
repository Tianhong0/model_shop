package org.majun.backend.service;

import org.majun.backend.dto.*;
import org.majun.backend.vo.*;

/**
 * 拼团服务接口
 */
public interface GroupBuyService {

    // ========== 管理端 ==========

    /**
     * 创建拼团活动
     */
    Long createActivity(GroupBuyActivityCreateRequest request);

    /**
     * 更新拼团活动
     */
    void updateActivity(GroupBuyActivityUpdateRequest request);

    /**
     * 分页查询拼团活动（管理端）
     */
    PageResult<GroupBuyActivityVO> listActivities(GroupBuyActivityQueryRequest request);

    /**
     * 获取活动详情（管理端）
     */
    GroupBuyActivityDetailVO getActivityDetail(Long activityId);

    /**
     * 更新活动状态
     */
    void updateActivityStatus(Long activityId, Integer status);

    /**
     * 获取批量打印折扣配置
     */
    java.util.List<org.majun.backend.entity.SysBatchPrintDiscount> getBatchDiscountList();

    /**
     * 保存批量打印折扣配置
     */
    void saveBatchDiscount(java.util.List<org.majun.backend.entity.SysBatchPrintDiscount> configList);

    // ========== 用户端 ==========

    /**
     * 获取拼团活动列表（用户端）
     */
    PageResult<GroupBuyActivityVO> listUserActivities(GroupBuyActivityQueryRequest request);

    /**
     * 获取活动详情（用户端）
     */
    GroupBuyActivityDetailVO getUserActivityDetail(Long activityId);

    /**
     * 发起拼团
     */
    GroupBuyCreateResponse createGroupBuy(Long userId, GroupBuyCreateRequest request);

    /**
     * 参与拼团
     */
    GroupBuyJoinResponse joinGroupBuy(Long userId, GroupBuyJoinRequest request);

    /**
     * 获取拼团详情
     */
    GroupBuyGroupDetailVO getGroupDetail(Long groupId, Long currentUserId);

    /**
     * 通过分享码获取拼团详情
     */
    GroupBuyGroupDetailVO getGroupByShareCode(String shareCode, Long currentUserId);

    /**
     * 获取我参与的拼团列表
     */
    PageResult<GroupBuyGroupVO> getMyGroups(Long userId, GroupBuyActivityQueryRequest request);

    /**
     * 获取活动下进行中的拼团列表
     */
    java.util.List<GroupBuyGroupVO> getOngoingGroupsByActivity(Long activityId, Integer limit);

    /**
     * 取消拼团
     */
    void cancelGroupBuy(Long userId, Long groupId);

    /**
     * 计算批量打印价格
     */
    BatchPriceResultVO calculateBatchPrice(BatchPriceCalculateRequest request);

    // ========== 支付相关 ==========

    /**
     * 为参与者创建订单
     */
    Long createOrderForParticipant(Long participantId, Long userId);

    /**
     * 参与者支付成功处理
     */
    void handleParticipantPaid(Long participantId);

    /**
     * 检查并处理拼团成功
     */
    void checkAndProcessGroupSuccess(Long groupId);

    // ========== 定时任务 ==========

    /**
     * 处理超时拼团
     */
    void processTimeoutGroups();

    /**
     * 处理活动状态更新
     */
    void processActivityStatus();
}
