package org.majun.backend.service;

import org.majun.backend.dto.InviteRelationQueryRequest;
import org.majun.backend.dto.PromotionRewardQueryRequest;
import org.majun.backend.dto.PromotionShareRequest;
import org.majun.backend.vo.InviteCodeVO;
import org.majun.backend.vo.InviteeVO;
import org.majun.backend.vo.PosterConfigVO;
import org.majun.backend.vo.PromotionCenterVO;
import org.majun.backend.vo.PromotionConfigVO;
import org.majun.backend.vo.PromotionRankVO;
import org.majun.backend.vo.PromotionRewardVO;
import org.majun.backend.vo.PageResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * 推广服务接口
 */
public interface PromotionService {

    /**
     * 获取或创建用户邀请码
     */
    InviteCodeVO getOrCreateInviteCode(Long userId);

    /**
     * 处理邀请注册
     */
    void handleInviteRegister(Long inviteeId, String inviteCode);

    /**
     * 处理首单奖励
     */
    void handleFirstOrderReward(Long inviteeId, Long orderId, String orderSn, BigDecimal orderAmount);

    /**
     * 处理消费返积分
     */
    void handleConsumeRebate(Long inviteeId, Long orderId, String orderSn, BigDecimal orderAmount);

    /**
     * 获取推广中心首页数据
     */
    PromotionCenterVO getPromotionCenter(Long userId);

    /**
     * 分页查询被邀请人列表
     */
    PageResult<InviteeVO> pageInvitees(InviteRelationQueryRequest request, Long userId);

    /**
     * 分页查询推广奖励记录
     */
    PageResult<PromotionRewardVO> pageRewards(PromotionRewardQueryRequest request, Long userId);

    /**
     * 记录分享行为
     */
    Long recordShare(PromotionShareRequest request, Long userId);

    /**
     * 生成推广海报
     */
    String generatePoster(Long userId);

    /**
     * 获取推广排行榜
     */
    List<PromotionRankVO> getRankList(Integer limit, String period);

    /**
     * 获取所有推广配置
     */
    List<PromotionConfigVO> getAllConfigs();

    /**
     * 更新推广配置
     */
    void updateConfig(String configKey, String configValue, String configDesc);

    /**
     * 获取海报配置（公开接口）
     */
    PosterConfigVO getPosterConfig();
}
