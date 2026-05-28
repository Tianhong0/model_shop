package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.dto.InviteRelationQueryRequest;
import org.majun.backend.dto.PromotionRewardQueryRequest;
import org.majun.backend.dto.PromotionShareRequest;
import org.majun.backend.entity.InviteRelation;
import org.majun.backend.entity.PromotionConfig;
import org.majun.backend.entity.PromotionReward;
import org.majun.backend.entity.PromotionShare;
import org.majun.backend.entity.SysUser;
import org.majun.backend.entity.UserInviteCode;
import org.majun.backend.enums.RewardType;
import org.majun.backend.repository.InviteRelationRepository;
import org.majun.backend.repository.PromotionConfigRepository;
import org.majun.backend.repository.PromotionRewardRepository;
import org.majun.backend.repository.PromotionShareRepository;
import org.majun.backend.repository.SysUserRepository;
import org.majun.backend.repository.UserInviteCodeRepository;
import org.majun.backend.service.PointService;
import org.majun.backend.service.PromotionService;
import org.majun.backend.vo.InviteCodeVO;
import org.majun.backend.vo.InviteeVO;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.PosterConfigVO;
import org.majun.backend.vo.PromotionCenterVO;
import org.majun.backend.vo.PromotionConfigVO;
import org.majun.backend.vo.PromotionRankVO;
import org.majun.backend.vo.PromotionRewardVO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private static final String BIZ_INVITE_REGISTER = "INVITE_REGISTER";
    private static final String BIZ_FIRST_ORDER = "FIRST_ORDER";
    private static final String BIZ_CONSUME_REBATE = "CONSUME_REBATE";
    private static final String LOCK_PREFIX = "promotion:invite_code:";
    private static final String RANK_KEY_PREFIX = "promotion:rank:";

    private final UserInviteCodeRepository inviteCodeRepository;
    private final InviteRelationRepository inviteRelationRepository;
    private final PromotionRewardRepository rewardRepository;
    private final PromotionShareRepository shareRepository;
    private final PromotionConfigRepository configRepository;
    private final SysUserRepository userRepository;
    private final PointService pointService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public InviteCodeVO getOrCreateInviteCode(Long userId) {
        UserInviteCode inviteCode = inviteCodeRepository.selectOne(
            new LambdaQueryWrapper<UserInviteCode>()
                .eq(UserInviteCode::getUserId, userId)
                .eq(UserInviteCode::getIsDelete, 0)
        );

        if (inviteCode == null) {
            inviteCode = createInviteCodeWithLock(userId);
        }

        return buildInviteCodeVO(inviteCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleInviteRegister(Long inviteeId, String inviteCode) {
        if (!StringUtils.hasText(inviteCode)) {
            return;
        }

        UserInviteCode code = inviteCodeRepository.selectOne(
            new LambdaQueryWrapper<UserInviteCode>()
                .eq(UserInviteCode::getInviteCode, inviteCode)
                .eq(UserInviteCode::getStatus, 1)
                .eq(UserInviteCode::getIsDelete, 0)
        );

        if (code == null || code.getUserId().equals(inviteeId)) {
            return;
        }

        Long existCount = inviteRelationRepository.selectCount(
            new LambdaQueryWrapper<InviteRelation>()
                .eq(InviteRelation::getInviteeId, inviteeId)
        );
        if (existCount > 0) {
            return;
        }

        InviteRelation relation = new InviteRelation();
        relation.setInviterId(code.getUserId());
        relation.setInviteeId(inviteeId);
        relation.setInviteCode(inviteCode);
        relation.setRegisterTime(LocalDateTime.now());
        relation.setTotalOrderCount(0);
        relation.setTotalOrderAmount(BigDecimal.ZERO);
        relation.setStatus(1);
        relation.setIsDelete(0);
        inviteRelationRepository.insert(relation);

        int rewardPoints = getConfigValueInt("INVITE_REGISTER_POINTS", 50);
        if (rewardPoints > 0) {
            String bizNo = "IR_" + inviteeId + "_" + System.currentTimeMillis();
            pointService.increase(code.getUserId(), rewardPoints, BIZ_INVITE_REGISTER,
                bizNo, inviteeId, "邀请好友注册奖励");

            saveRewardRecord(code.getUserId(), relation.getId(), RewardType.INVITE_REGISTER,
                rewardPoints, "REGISTER", inviteeId, null);

            updateInviteCodeStats(code.getId(), 1, rewardPoints);
        }

        updateUserInviter(inviteeId, code.getUserId(), code.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleFirstOrderReward(Long inviteeId, Long orderId, String orderSn, BigDecimal orderAmount) {
        InviteRelation relation = inviteRelationRepository.selectOne(
            new LambdaQueryWrapper<InviteRelation>()
                .eq(InviteRelation::getInviteeId, inviteeId)
                .eq(InviteRelation::getStatus, 1)
                .eq(InviteRelation::getIsDelete, 0)
        );

        if (relation == null || relation.getFirstOrderId() != null) {
            return;
        }

        relation.setFirstOrderTime(LocalDateTime.now());
        relation.setFirstOrderId(orderId);
        relation.setTotalOrderCount(1);
        relation.setTotalOrderAmount(orderAmount);
        inviteRelationRepository.updateById(relation);

        int rewardPoints = getConfigValueInt("FIRST_ORDER_POINTS", 100);
        if (rewardPoints > 0) {
            String bizNo = "FO_" + orderId;
            pointService.increase(relation.getInviterId(), rewardPoints, BIZ_FIRST_ORDER,
                bizNo, orderId, "邀请好友首单奖励");

            saveRewardRecord(relation.getInviterId(), relation.getId(), RewardType.FIRST_ORDER,
                rewardPoints, "ORDER", orderId, orderAmount);

            UserInviteCode code = inviteCodeRepository.selectOne(
                new LambdaQueryWrapper<UserInviteCode>()
                    .eq(UserInviteCode::getUserId, relation.getInviterId())
            );
            if (code != null) {
                updateInviteCodeStats(code.getId(), 0, rewardPoints);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleConsumeRebate(Long inviteeId, Long orderId, String orderSn, BigDecimal orderAmount) {
        InviteRelation relation = inviteRelationRepository.selectOne(
            new LambdaQueryWrapper<InviteRelation>()
                .eq(InviteRelation::getInviteeId, inviteeId)
                .eq(InviteRelation::getStatus, 1)
                .eq(InviteRelation::getIsDelete, 0)
        );

        if (relation == null) {
            return;
        }

        BigDecimal rebateRate = new BigDecimal(getConfigValue("CONSUME_REBATE_RATE", "0.01"));
        int maxRebatePoints = getConfigValueInt("MAX_REBATE_POINTS", 500);

        int rebatePoints = orderAmount.multiply(rebateRate)
            .setScale(0, RoundingMode.DOWN)
            .intValue();
        rebatePoints = Math.min(rebatePoints, maxRebatePoints);

        if (rebatePoints <= 0) {
            return;
        }

        Long existCount = rewardRepository.selectCount(
            new LambdaQueryWrapper<PromotionReward>()
                .eq(PromotionReward::getUserId, relation.getInviterId())
                .eq(PromotionReward::getRewardType, RewardType.CONSUME_REBATE.getCode())
                .eq(PromotionReward::getRefId, orderId)
        );
        if (existCount > 0) {
            return;
        }

        String bizNo = "CR_" + orderId;
        pointService.increase(relation.getInviterId(), rebatePoints, BIZ_CONSUME_REBATE,
            bizNo, orderId, "好友消费返积分");

        saveRewardRecord(relation.getInviterId(), relation.getId(), RewardType.CONSUME_REBATE,
            rebatePoints, "ORDER", orderId, orderAmount);

        relation.setTotalOrderCount(relation.getTotalOrderCount() + 1);
        relation.setTotalOrderAmount(relation.getTotalOrderAmount().add(orderAmount));
        inviteRelationRepository.updateById(relation);

        UserInviteCode code = inviteCodeRepository.selectOne(
            new LambdaQueryWrapper<UserInviteCode>()
                .eq(UserInviteCode::getUserId, relation.getInviterId())
        );
        if (code != null) {
            updateInviteCodeStats(code.getId(), 0, rebatePoints);
        }
    }

    @Override
    public PromotionCenterVO getPromotionCenter(Long userId) {
        InviteCodeVO inviteCode = getOrCreateInviteCode(userId);

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        Long todayInvited = inviteRelationRepository.selectCount(
            new LambdaQueryWrapper<InviteRelation>()
                .eq(InviteRelation::getInviterId, userId)
                .ge(InviteRelation::getCreateTime, todayStart)
        );

        List<PromotionReward> todayRewards = rewardRepository.selectList(
            new LambdaQueryWrapper<PromotionReward>()
                .eq(PromotionReward::getUserId, userId)
                .ge(PromotionReward::getCreateTime, todayStart)
        );
        int todayPoints = todayRewards.stream()
            .mapToInt(r -> r.getRewardPoints() != null ? r.getRewardPoints() : 0)
            .sum();

        UserInviteCode code = inviteCodeRepository.selectOne(
            new LambdaQueryWrapper<UserInviteCode>()
                .eq(UserInviteCode::getUserId, userId)
        );

        List<InviteRelation> relations = inviteRelationRepository.selectList(
            new LambdaQueryWrapper<InviteRelation>()
                .eq(InviteRelation::getInviterId, userId)
        );

        int totalOrders = relations.stream()
            .mapToInt(r -> r.getTotalOrderCount() != null ? r.getTotalOrderCount() : 0)
            .sum();

        BigDecimal totalOrderAmount = relations.stream()
            .map(r -> r.getTotalOrderAmount() != null ? r.getTotalOrderAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PromotionCenterVO.builder()
            .inviteCode(inviteCode)
            .todayInvited(todayInvited != null ? todayInvited.intValue() : 0)
            .todayPoints(todayPoints)
            .totalInvited(code != null ? code.getTotalInvited() : 0)
            .totalPoints(code != null ? code.getTotalPointsEarned() : 0)
            .totalOrders(totalOrders)
            .totalOrderAmount(totalOrderAmount)
            .build();
    }

    @Override
    public PageResult<InviteeVO> pageInvitees(InviteRelationQueryRequest request, Long userId) {
        LambdaQueryWrapper<InviteRelation> wrapper = new LambdaQueryWrapper<InviteRelation>()
            .eq(InviteRelation::getInviterId, userId)
            .eq(InviteRelation::getIsDelete, 0)
            .orderByDesc(InviteRelation::getCreateTime);

        if (StringUtils.hasText(request.getStartTime())) {
            wrapper.ge(InviteRelation::getCreateTime, LocalDate.parse(request.getStartTime()).atStartOfDay());
        }
        if (StringUtils.hasText(request.getEndTime())) {
            wrapper.lt(InviteRelation::getCreateTime, LocalDate.parse(request.getEndTime()).plusDays(1).atStartOfDay());
        }

        Page<InviteRelation> page = new Page<>(request.getPageNum(), request.getPageSize());
        inviteRelationRepository.selectPage(page, wrapper);

        List<InviteeVO> records = page.getRecords().stream()
            .map(this::toInviteeVO)
            .collect(Collectors.toList());

        return PageResult.<InviteeVO>builder()
            .records(records)
            .total(page.getTotal())
            .pageNum((int) page.getCurrent())
            .pageSize((int) page.getSize())
            .pages((int) page.getPages())
            .build();
    }

    @Override
    public PageResult<PromotionRewardVO> pageRewards(PromotionRewardQueryRequest request, Long userId) {
        LambdaQueryWrapper<PromotionReward> wrapper = new LambdaQueryWrapper<PromotionReward>()
            .eq(PromotionReward::getUserId, userId)
            .eq(PromotionReward::getIsDelete, 0)
            .orderByDesc(PromotionReward::getCreateTime);

        if (StringUtils.hasText(request.getRewardType())) {
            wrapper.eq(PromotionReward::getRewardType, request.getRewardType());
        }
        if (StringUtils.hasText(request.getStartTime())) {
            wrapper.ge(PromotionReward::getCreateTime, LocalDate.parse(request.getStartTime()).atStartOfDay());
        }
        if (StringUtils.hasText(request.getEndTime())) {
            wrapper.lt(PromotionReward::getCreateTime, LocalDate.parse(request.getEndTime()).plusDays(1).atStartOfDay());
        }

        Page<PromotionReward> page = new Page<>(request.getPageNum(), request.getPageSize());
        rewardRepository.selectPage(page, wrapper);

        List<PromotionRewardVO> records = page.getRecords().stream()
            .map(this::toRewardVO)
            .collect(Collectors.toList());

        return PageResult.<PromotionRewardVO>builder()
            .records(records)
            .total(page.getTotal())
            .pageNum((int) page.getCurrent())
            .pageSize((int) page.getSize())
            .pages((int) page.getPages())
            .build();
    }

    @Override
    public Long recordShare(PromotionShareRequest request, Long userId) {
        PromotionShare share = new PromotionShare();
        share.setUserId(userId);
        share.setShareType(request.getShareType());
        share.setShareChannel(request.getShareChannel());
        share.setRefType(request.getRefType());
        share.setRefId(request.getRefId());
        share.setShareUrl(request.getShareUrl());
        share.setPosterUrl(request.getPosterUrl());
        share.setClickCount(0);
        share.setConvertCount(0);
        share.setIsDelete(0);
        shareRepository.insert(share);
        return share.getId();
    }

    @Override
    public String generatePoster(Long userId) {
        InviteCodeVO inviteCode = getOrCreateInviteCode(userId);
        return inviteCode.getPosterUrl();
    }

    @Override
    public List<PromotionRankVO> getRankList(Integer limit, String period) {
        String rankKey = RANK_KEY_PREFIX + period;

        java.util.Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> tuples =
            redisTemplate.opsForZSet().reverseRangeWithScores(rankKey, 0, limit - 1);

        if (tuples == null || tuples.isEmpty()) {
            List<UserInviteCode> topCodes = inviteCodeRepository.selectList(
                new LambdaQueryWrapper<UserInviteCode>()
                    .eq(UserInviteCode::getStatus, 1)
                    .eq(UserInviteCode::getIsDelete, 0)
                    .orderByDesc(UserInviteCode::getTotalInvited)
                    .last("LIMIT " + limit)
            );

            List<PromotionRankVO> result = new ArrayList<>();
            int rank = 1;
            for (UserInviteCode code : topCodes) {
                SysUser user = userRepository.selectById(code.getUserId());
                if (user != null) {
                    result.add(PromotionRankVO.builder()
                        .rank(rank++)
                        .userId(String.valueOf(user.getId()))
                        .nickname(user.getNickname())
                        .avatar(user.getAvatar())
                        .inviteCount(code.getTotalInvited())
                        .totalPoints(code.getTotalPointsEarned())
                        .build());
                }
            }
            return result;
        }

        List<PromotionRankVO> result = new ArrayList<>();
        int rank = 1;
        for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object> tuple : tuples) {
            Long userId = Long.valueOf(tuple.getValue().toString());
            SysUser user = userRepository.selectById(userId);
            if (user != null) {
                result.add(PromotionRankVO.builder()
                    .rank(rank++)
                    .userId(String.valueOf(userId))
                    .nickname(user.getNickname())
                    .avatar(user.getAvatar())
                    .inviteCount(tuple.getScore() != null ? tuple.getScore().intValue() : 0)
                    .totalPoints(0)
                    .build());
            }
        }
        return result;
    }

    private UserInviteCode createInviteCodeWithLock(Long userId) {
        // 先检查是否已存在
        UserInviteCode existing = inviteCodeRepository.selectOne(
            new LambdaQueryWrapper<UserInviteCode>()
                .eq(UserInviteCode::getUserId, userId)
                .eq(UserInviteCode::getIsDelete, 0)
        );
        if (existing != null) {
            return existing;
        }

        String lockKey = LOCK_PREFIX + userId;
        try {
            // 尝试获取分布式锁
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 30, TimeUnit.SECONDS);
            if (!Boolean.TRUE.equals(acquired)) {
                // 锁获取失败，等待其他线程完成后重新查询
                Thread.sleep(200);
                UserInviteCode code = inviteCodeRepository.selectOne(
                    new LambdaQueryWrapper<UserInviteCode>()
                        .eq(UserInviteCode::getUserId, userId)
                        .eq(UserInviteCode::getIsDelete, 0)
                );
                if (code != null) {
                    return code;
                }
                // 如果还是没有，抛出异常让调用者重试
                throw new BusinessException("系统繁忙，请稍后重试");
            }

            // 再次检查（双重检查）
            UserInviteCode doubleCheck = inviteCodeRepository.selectOne(
                new LambdaQueryWrapper<UserInviteCode>()
                    .eq(UserInviteCode::getUserId, userId)
                    .eq(UserInviteCode::getIsDelete, 0)
            );
            if (doubleCheck != null) {
                return doubleCheck;
            }

            // 生成唯一邀请码
            String code = generateUniqueCode();

            UserInviteCode newCode = new UserInviteCode();
            newCode.setUserId(userId);
            newCode.setInviteCode(code);
            newCode.setTotalInvited(0);
            newCode.setTotalPointsEarned(0);
            newCode.setStatus(1);
            newCode.setIsDelete(0);

            try {
                inviteCodeRepository.insert(newCode);
                return newCode;
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 并发插入导致的唯一键冲突，重新查询返回
                log.info("邀请码并发创建冲突, userId={}", userId);
                UserInviteCode codeExists = inviteCodeRepository.selectOne(
                    new LambdaQueryWrapper<UserInviteCode>()
                        .eq(UserInviteCode::getUserId, userId)
                        .eq(UserInviteCode::getIsDelete, 0)
                );
                if (codeExists != null) {
                    return codeExists;
                }
                throw new BusinessException("创建邀请码失败，请重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("系统繁忙，请重试");
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private String generateUniqueCode() {
        int codeLength = getConfigValueInt("INVITE_CODE_LENGTH", 6);
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

        for (int i = 0; i < 100; i++) {
            StringBuilder sb = new StringBuilder();
            Random random = new Random();
            for (int j = 0; j < codeLength; j++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            String code = sb.toString();

            Long count = inviteCodeRepository.selectCount(
                new LambdaQueryWrapper<UserInviteCode>()
                    .eq(UserInviteCode::getInviteCode, code)
            );
            if (count == null || count == 0) {
                return code;
            }
        }
        throw new BusinessException("生成邀请码失败，请重试");
    }

    private String getConfigValue(String key, String defaultValue) {
        PromotionConfig config = configRepository.selectOne(
            new LambdaQueryWrapper<PromotionConfig>()
                .eq(PromotionConfig::getConfigKey, key)
                .eq(PromotionConfig::getStatus, 1)
        );
        if (config == null || !StringUtils.hasText(config.getConfigValue())) {
            return defaultValue;
        }
        return config.getConfigValue();
    }

    private int getConfigValueInt(String key, int defaultValue) {
        String value = getConfigValue(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void updateInviteCodeStats(Long codeId, int invitedIncrement, int pointsIncrement) {
        UserInviteCode code = inviteCodeRepository.selectById(codeId);
        if (code != null) {
            code.setTotalInvited(code.getTotalInvited() + invitedIncrement);
            code.setTotalPointsEarned(code.getTotalPointsEarned() + pointsIncrement);
            inviteCodeRepository.updateById(code);
        }
    }

    private void saveRewardRecord(Long userId, Long relationId, RewardType type,
            int points, String refType, Long refId, BigDecimal refAmount) {
        PromotionReward reward = new PromotionReward();
        reward.setUserId(userId);
        reward.setInviteRelationId(relationId);
        reward.setRewardType(type.getCode());
        reward.setRewardPoints(points);
        reward.setRefType(refType);
        reward.setRefId(refId);
        reward.setRefAmount(refAmount);
        reward.setStatus(1);
        reward.setRemark(type.getDescription());
        reward.setIsDelete(0);
        rewardRepository.insert(reward);
    }

    private void updateUserInviter(Long inviteeId, Long inviterId, Long inviteCodeId) {
        SysUser user = userRepository.selectById(inviteeId);
        if (user != null) {
            user.setInviterId(inviterId);
            user.setInviteCodeId(inviteCodeId);
            userRepository.updateById(user);
        }
    }

    private InviteCodeVO buildInviteCodeVO(UserInviteCode inviteCode) {
        String inviteLink = generateInviteLink(inviteCode.getInviteCode());
        String posterUrl = generatePosterUrl(inviteCode.getInviteCode());

        return InviteCodeVO.builder()
            .inviteCode(inviteCode.getInviteCode())
            .totalInvited(inviteCode.getTotalInvited())
            .totalPointsEarned(inviteCode.getTotalPointsEarned())
            .inviteLink(inviteLink)
            .posterUrl(posterUrl)
            .build();
    }

    private String generateInviteLink(String inviteCode) {
        // 尝试从配置中获取域名，如果没有配置则使用默认值
        String baseUrl = getConfigValue("PROMOTION_BASE_URL", "");
        if (!StringUtils.hasText(baseUrl)) {
            // 默认使用当前服务器的地址
            baseUrl = "https://your-domain.com";
        }
        return baseUrl + "/invite/" + inviteCode;
    }

    private String generatePosterUrl(String inviteCode) {
        String baseUrl = getConfigValue("PROMOTION_BASE_URL", "");
        if (!StringUtils.hasText(baseUrl)) {
            baseUrl = "https://your-domain.com";
        }
        return baseUrl + "/api/promotion/poster/image?code=" + inviteCode;
    }

    private InviteeVO toInviteeVO(InviteRelation relation) {
        SysUser invitee = userRepository.selectById(relation.getInviteeId());

        Long contributedPoints = rewardRepository.selectList(
            new LambdaQueryWrapper<PromotionReward>()
                .eq(PromotionReward::getInviteRelationId, relation.getId())
                .eq(PromotionReward::getStatus, 1)
        ).stream()
            .mapToLong(r -> r.getRewardPoints() != null ? r.getRewardPoints() : 0L)
            .sum();

        return InviteeVO.builder()
            .userId(invitee != null ? String.valueOf(invitee.getId()) : null)
            .nickname(invitee != null ? invitee.getNickname() : null)
            .avatar(invitee != null ? invitee.getAvatar() : null)
            .registerTime(relation.getRegisterTime())
            .firstOrderTime(relation.getFirstOrderTime())
            .orderCount(relation.getTotalOrderCount())
            .orderAmount(relation.getTotalOrderAmount())
            .contributedPoints(contributedPoints.intValue())
            .build();
    }

    private PromotionRewardVO toRewardVO(PromotionReward reward) {
        RewardType type = RewardType.fromCode(reward.getRewardType());

        return PromotionRewardVO.builder()
            .id(String.valueOf(reward.getId()))
            .rewardType(reward.getRewardType())
            .rewardTypeDesc(type != null ? type.getDescription() : "")
            .rewardPoints(reward.getRewardPoints())
            .refAmount(reward.getRefAmount())
            .remark(reward.getRemark())
            .createTime(reward.getCreateTime())
            .build();
    }

    @Override
    public List<PromotionConfigVO> getAllConfigs() {
        List<PromotionConfig> configs = configRepository.selectList(
            new LambdaQueryWrapper<PromotionConfig>()
                .eq(PromotionConfig::getIsDelete, 0)
                .orderByAsc(PromotionConfig::getId)
        );

        return configs.stream()
            .map(this::toConfigVO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(String configKey, String configValue, String configDesc) {
        PromotionConfig config = configRepository.selectOne(
            new LambdaQueryWrapper<PromotionConfig>()
                .eq(PromotionConfig::getConfigKey, configKey)
                .eq(PromotionConfig::getIsDelete, 0)
        );

        if (config == null) {
            config = new PromotionConfig();
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            config.setConfigDesc(configDesc);
            config.setStatus(1);
            config.setIsDelete(0);
            configRepository.insert(config);
        } else {
            config.setConfigValue(configValue);
            if (StringUtils.hasText(configDesc)) {
                config.setConfigDesc(configDesc);
            }
            configRepository.updateById(config);
        }
    }

    @Override
    public PosterConfigVO getPosterConfig() {
        return PosterConfigVO.builder()
            .title(getConfigValue("POSTER_TITLE", "印力无限"))
            .subtitle(getConfigValue("POSTER_SUBTITLE", "邀请好友注册，双方均可获得积分奖励"))
            .bgColorStart(getConfigValue("POSTER_BG_COLOR_START", "#00bfff"))
            .bgColorEnd(getConfigValue("POSTER_BG_COLOR_END", "#0099cc"))
            .bgImage(getConfigValue("POSTER_BG_IMAGE", ""))
            .titleColor(getConfigValue("POSTER_TITLE_COLOR", "#1a2030"))
            .codeColor(getConfigValue("POSTER_CODE_COLOR", "#00bfff"))
            .tipsText(getConfigValue("POSTER_TIPS_TEXT", "长按保存图片，分享给好友"))
            .width(getConfigValueInt("POSTER_WIDTH", 200))
            .height(getConfigValueInt("POSTER_HEIGHT", 280))
            .qrcodeSize(getConfigValueInt("POSTER_QRCODE_SIZE", 70))
            .inviteRegisterPoints(getConfigValueInt("INVITE_REGISTER_POINTS", 50))
            .firstOrderPoints(getConfigValueInt("FIRST_ORDER_POINTS", 100))
            .consumeRebateRate(getConfigValue("CONSUME_REBATE_RATE", "0.01"))
            .build();
    }

    private PromotionConfigVO toConfigVO(PromotionConfig config) {
        PromotionConfigVO vo = new PromotionConfigVO();
        vo.setId(config.getId());
        vo.setConfigKey(config.getConfigKey());
        vo.setConfigValue(config.getConfigValue());
        vo.setConfigDesc(config.getConfigDesc());
        vo.setStatus(config.getStatus());
        vo.setCreateTime(config.getCreateTime());
        vo.setUpdateTime(config.getUpdateTime());
        return vo;
    }
}
