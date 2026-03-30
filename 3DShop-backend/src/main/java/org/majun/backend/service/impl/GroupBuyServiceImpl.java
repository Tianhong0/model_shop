package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.dto.*;
import org.majun.backend.entity.*;
import org.majun.backend.enums.GroupBuyDiscountType;
import org.majun.backend.enums.GroupBuyStatus;
import org.majun.backend.enums.ParticipantStatus;
import org.majun.backend.repository.*;
import org.majun.backend.service.GroupBuyService;
import org.majun.backend.vo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 拼团服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupBuyServiceImpl implements GroupBuyService {

    private final SysGroupBuyActivityRepository activityRepository;
    private final SysGroupBuyGroupRepository groupRepository;
    private final SysGroupBuyParticipantRepository participantRepository;
    private final SysBatchPrintDiscountRepository batchDiscountRepository;
    private final SysModelRepository modelRepository;
    private final SysModelImageRepository modelImageRepository;
    private final SysUserRepository userRepository;
    private final ModelMaterialRepository materialRepository;
    private final SysOrderRepository orderRepository;

    private static final String GROUP_SN_PREFIX = "GB";
    private static final int SHARE_CODE_LENGTH = 6;
    private static final String ORDER_SN_PREFIX = "ORD";

    // ========== 管理端 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createActivity(GroupBuyActivityCreateRequest request) {
        // 验证模型存在
        SysModel model = modelRepository.selectById(request.getModelId());
        if (model == null) {
            throw new BusinessException("模型不存在");
        }

        // 验证时间
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new BusinessException("结束时间不能早于开始时间");
        }

        // 验证价格
        if (request.getGroupPrice().compareTo(request.getOriginalPrice()) > 0) {
            throw new BusinessException("拼团价不能高于原价");
        }

        SysGroupBuyActivity activity = new SysGroupBuyActivity();
        activity.setActivityName(request.getActivityName());
        activity.setModelId(request.getModelId());
        activity.setMinPeople(request.getMinPeople());
        activity.setMaxPeople(request.getMaxPeople());
        activity.setDiscountType(request.getDiscountType());
        activity.setDiscountValue(request.getDiscountValue());
        activity.setLadderConfig(request.getLadderConfig());
        activity.setOriginalPrice(request.getOriginalPrice());
        activity.setGroupPrice(request.getGroupPrice());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setTimeoutHours(request.getTimeoutHours());
        activity.setStatus(1); // 默认启用
        activity.setTotalStock(request.getTotalStock());
        activity.setSoldCount(0);
        activity.setCoverImage(request.getCoverImage());
        activity.setDescription(request.getDescription());
        activity.setIsDelete(0);

        activityRepository.insert(activity);
        return activity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateActivity(GroupBuyActivityUpdateRequest request) {
        SysGroupBuyActivity activity = activityRepository.selectById(request.getId());
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }

        if (StringUtils.hasText(request.getActivityName())) {
            activity.setActivityName(request.getActivityName());
        }
        if (request.getMinPeople() != null) {
            activity.setMinPeople(request.getMinPeople());
        }
        if (request.getMaxPeople() != null) {
            activity.setMaxPeople(request.getMaxPeople());
        }
        if (request.getDiscountType() != null) {
            activity.setDiscountType(request.getDiscountType());
        }
        if (request.getDiscountValue() != null) {
            activity.setDiscountValue(request.getDiscountValue());
        }
        if (request.getLadderConfig() != null) {
            activity.setLadderConfig(request.getLadderConfig());
        }
        if (request.getOriginalPrice() != null) {
            activity.setOriginalPrice(request.getOriginalPrice());
        }
        if (request.getGroupPrice() != null) {
            activity.setGroupPrice(request.getGroupPrice());
        }
        if (request.getStartTime() != null) {
            activity.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            activity.setEndTime(request.getEndTime());
        }
        if (request.getTimeoutHours() != null) {
            activity.setTimeoutHours(request.getTimeoutHours());
        }
        if (request.getTotalStock() != null) {
            activity.setTotalStock(request.getTotalStock());
        }
        if (request.getCoverImage() != null) {
            activity.setCoverImage(request.getCoverImage());
        }
        if (request.getDescription() != null) {
            activity.setDescription(request.getDescription());
        }

        activityRepository.updateById(activity);
    }

    @Override
    public PageResult<GroupBuyActivityVO> listActivities(GroupBuyActivityQueryRequest request) {
        LambdaQueryWrapper<SysGroupBuyActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysGroupBuyActivity::getIsDelete, 0);

        if (StringUtils.hasText(request.getActivityName())) {
            wrapper.like(SysGroupBuyActivity::getActivityName, request.getActivityName());
        }
        if (request.getModelId() != null) {
            wrapper.eq(SysGroupBuyActivity::getModelId, request.getModelId());
        }
        if (request.getStatus() != null) {
            wrapper.eq(SysGroupBuyActivity::getStatus, request.getStatus());
        }

        wrapper.orderByDesc(SysGroupBuyActivity::getCreateTime);

        Page<SysGroupBuyActivity> page = new Page<>(request.getPageNum(), request.getPageSize());
        activityRepository.selectPage(page, wrapper);

        List<GroupBuyActivityVO> records = page.getRecords().stream()
                .map(this::toActivityVO)
                .collect(Collectors.toList());

        return PageResult.<GroupBuyActivityVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    public GroupBuyActivityDetailVO getActivityDetail(Long activityId) {
        SysGroupBuyActivity activity = activityRepository.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        return toActivityDetailVO(activity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateActivityStatus(Long activityId, Integer status) {
        SysGroupBuyActivity activity = activityRepository.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        activity.setStatus(status);
        activityRepository.updateById(activity);
    }

    @Override
    public List<SysBatchPrintDiscount> getBatchDiscountList() {
        return batchDiscountRepository.selectList(
                new LambdaQueryWrapper<SysBatchPrintDiscount>()
                        .orderByAsc(SysBatchPrintDiscount::getSortOrder)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatchDiscount(List<SysBatchPrintDiscount> configList) {
        // 先删除所有配置
        batchDiscountRepository.delete(new LambdaQueryWrapper<>());
        // 再批量插入
        for (SysBatchPrintDiscount config : configList) {
            batchDiscountRepository.insert(config);
        }
    }

    // ========== 用户端 ==========

    @Override
    public PageResult<GroupBuyActivityVO> listUserActivities(GroupBuyActivityQueryRequest request) {
        LambdaQueryWrapper<SysGroupBuyActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysGroupBuyActivity::getIsDelete, 0);
        wrapper.eq(SysGroupBuyActivity::getStatus, 1); // 只查询启用的
        wrapper.le(SysGroupBuyActivity::getStartTime, LocalDateTime.now());
        wrapper.ge(SysGroupBuyActivity::getEndTime, LocalDateTime.now());

        if (StringUtils.hasText(request.getActivityName())) {
            wrapper.like(SysGroupBuyActivity::getActivityName, request.getActivityName());
        }
        if (request.getModelId() != null) {
            wrapper.eq(SysGroupBuyActivity::getModelId, request.getModelId());
        }

        wrapper.orderByDesc(SysGroupBuyActivity::getCreateTime);

        Page<SysGroupBuyActivity> page = new Page<>(request.getPageNum(), request.getPageSize());
        activityRepository.selectPage(page, wrapper);

        List<GroupBuyActivityVO> records = page.getRecords().stream()
                .map(this::toActivityVO)
                .collect(Collectors.toList());

        return PageResult.<GroupBuyActivityVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    public GroupBuyActivityDetailVO getUserActivityDetail(Long activityId) {
        return getActivityDetail(activityId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupBuyCreateResponse createGroupBuy(Long userId, GroupBuyCreateRequest request) {
        // 1. 验证活动有效性
        SysGroupBuyActivity activity = validateActivity(request.getActivityId());

        // 2. 检查库存
        if (activity.getTotalStock() != null && activity.getSoldCount() >= activity.getTotalStock()) {
            throw new BusinessException("活动库存不足");
        }

        // 3. 检查用户是否已参与该活动的其他进行中拼团
        checkUserExistingGroup(userId, request.getActivityId());

        // 4. 计算价格
        BigDecimal unitPrice = calculateUnitPrice(activity, request.getQuantity());
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        // 5. 创建拼团组
        SysGroupBuyGroup group = new SysGroupBuyGroup();
        group.setActivityId(activity.getId());
        group.setLeaderUserId(userId);
        group.setGroupSn(generateGroupSn());
        group.setShareCode(generateShareCode());
        group.setCurrentPeople(1);
        group.setTargetPeople(activity.getMinPeople());
        group.setStatus(GroupBuyStatus.IN_PROGRESS.getCode());
        group.setTotalAmount(totalAmount);
        group.setExpireTime(LocalDateTime.now().plusHours(activity.getTimeoutHours()));
        group.setIsDelete(0);
        groupRepository.insert(group);

        // 6. 创建参与记录
        SysGroupBuyParticipant participant = createParticipant(
                group, activity, userId, request, unitPrice, totalAmount, true
        );

        // 7. 构建响应
        return GroupBuyCreateResponse.builder()
                .groupId(group.getId())
                .groupSn(group.getGroupSn())
                .shareCode(group.getShareCode())
                .participantId(participant.getId())
                .unitPrice(unitPrice)
                .totalAmount(totalAmount)
                .expireTime(group.getExpireTime())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupBuyJoinResponse joinGroupBuy(Long userId, GroupBuyJoinRequest request) {
        // 1. 验证拼团组
        SysGroupBuyGroup group = groupRepository.selectById(request.getGroupId());
        if (group == null) {
            throw new BusinessException("拼团不存在");
        }

        // 2. 验证活动
        SysGroupBuyActivity activity = activityRepository.selectById(group.getActivityId());
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }

        // 3. 检查拼团状态
        if (!GroupBuyStatus.IN_PROGRESS.getCode().equals(group.getStatus())) {
            throw new BusinessException("该拼团已结束");
        }

        // 4. 检查是否过期
        if (LocalDateTime.now().isAfter(group.getExpireTime())) {
            throw new BusinessException("该拼团已过期");
        }

        // 5. 检查人数限制
        if (activity.getMaxPeople() != null && activity.getMaxPeople() > 0
                && group.getCurrentPeople() >= activity.getMaxPeople()) {
            throw new BusinessException("该拼团人数已满");
        }

        // 6. 检查用户是否已参与
        Long existingCount = participantRepository.selectCount(
                new LambdaQueryWrapper<SysGroupBuyParticipant>()
                        .eq(SysGroupBuyParticipant::getGroupId, group.getId())
                        .eq(SysGroupBuyParticipant::getUserId, userId)
                        .ne(SysGroupBuyParticipant::getStatus, ParticipantStatus.CANCELED.getCode())
        );
        if (existingCount > 0) {
            throw new BusinessException("您已参与该拼团");
        }

        // 7. 计算价格
        BigDecimal unitPrice = calculateUnitPrice(activity, request.getQuantity());
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        // 8. 创建参与记录
        SysGroupBuyParticipant participant = createParticipant(
                group, activity, userId, request, unitPrice, totalAmount, false
        );

        // 9. 更新拼团组人数
        groupRepository.update(null, new LambdaUpdateWrapper<SysGroupBuyGroup>()
                .eq(SysGroupBuyGroup::getId, group.getId())
                .set(SysGroupBuyGroup::getCurrentPeople, group.getCurrentPeople() + 1)
                .setSql("total_amount = total_amount + " + totalAmount)
        );

        return GroupBuyJoinResponse.builder()
                .groupId(group.getId())
                .participantId(participant.getId())
                .unitPrice(unitPrice)
                .totalAmount(totalAmount)
                .currentPeople(group.getCurrentPeople() + 1)
                .targetPeople(group.getTargetPeople())
                .build();
    }

    @Override
    public GroupBuyGroupDetailVO getGroupDetail(Long groupId, Long currentUserId) {
        SysGroupBuyGroup group = groupRepository.selectById(groupId);
        if (group == null) {
            throw new BusinessException("拼团不存在");
        }

        return buildGroupDetailVO(group, currentUserId);
    }

    @Override
    public GroupBuyGroupDetailVO getGroupByShareCode(String shareCode, Long currentUserId) {
        SysGroupBuyGroup group = groupRepository.selectOne(
                new LambdaQueryWrapper<SysGroupBuyGroup>()
                        .eq(SysGroupBuyGroup::getShareCode, shareCode)
        );
        if (group == null) {
            throw new BusinessException("拼团不存在");
        }

        return buildGroupDetailVO(group, currentUserId);
    }

    @Override
    public PageResult<GroupBuyGroupVO> getMyGroups(Long userId, GroupBuyActivityQueryRequest request) {
        // 查询用户参与的拼团ID
        List<SysGroupBuyParticipant> participants = participantRepository.selectList(
                new LambdaQueryWrapper<SysGroupBuyParticipant>()
                        .eq(SysGroupBuyParticipant::getUserId, userId)
                        .ne(SysGroupBuyParticipant::getStatus, ParticipantStatus.CANCELED.getCode())
                        .orderByDesc(SysGroupBuyParticipant::getCreateTime)
        );

        if (participants.isEmpty()) {
            return PageResult.<GroupBuyGroupVO>builder()
                    .records(Collections.emptyList())
                    .total(0L)
                    .pageNum(request.getPageNum())
                    .pageSize(request.getPageSize())
                    .pages(0)
                    .build();
        }

        Set<Long> groupIds = participants.stream()
                .map(SysGroupBuyParticipant::getGroupId)
                .collect(Collectors.toSet());

        // 分页查询拼团组
        Page<SysGroupBuyGroup> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<SysGroupBuyGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysGroupBuyGroup::getId, groupIds);
        wrapper.orderByDesc(SysGroupBuyGroup::getCreateTime);

        groupRepository.selectPage(page, wrapper);

        List<GroupBuyGroupVO> records = page.getRecords().stream()
                .map(this::toGroupVO)
                .collect(Collectors.toList());

        return PageResult.<GroupBuyGroupVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    public List<GroupBuyGroupVO> getOngoingGroupsByActivity(Long activityId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }

        // 查询进行中的拼团组
        List<SysGroupBuyGroup> groups = groupRepository.selectList(
                new LambdaQueryWrapper<SysGroupBuyGroup>()
                        .eq(SysGroupBuyGroup::getActivityId, activityId)
                        .eq(SysGroupBuyGroup::getStatus, GroupBuyStatus.IN_PROGRESS.getCode())
                        .gt(SysGroupBuyGroup::getExpireTime, LocalDateTime.now())
                        .orderByAsc(SysGroupBuyGroup::getCreateTime)
                        .last("LIMIT " + limit)
        );

        return groups.stream()
                .map(this::toGroupVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelGroupBuy(Long userId, Long groupId) {
        SysGroupBuyGroup group = groupRepository.selectById(groupId);
        if (group == null) {
            throw new BusinessException("拼团不存在");
        }

        // 只有团长可以取消
        if (!group.getLeaderUserId().equals(userId)) {
            throw new BusinessException("只有团长可以取消拼团");
        }

        // 只能取消进行中的拼团
        if (!GroupBuyStatus.IN_PROGRESS.getCode().equals(group.getStatus())) {
            throw new BusinessException("该拼团已结束，无法取消");
        }

        // 更新拼团状态
        group.setStatus(GroupBuyStatus.CANCELED.getCode());
        group.setFailTime(LocalDateTime.now());
        group.setFailReason("团长取消");
        groupRepository.updateById(group);

        // 取消所有参与记录
        participantRepository.update(null, new LambdaUpdateWrapper<SysGroupBuyParticipant>()
                .eq(SysGroupBuyParticipant::getGroupId, groupId)
                .eq(SysGroupBuyParticipant::getStatus, ParticipantStatus.PENDING_PAYMENT.getCode())
                .set(SysGroupBuyParticipant::getStatus, ParticipantStatus.CANCELED.getCode())
        );
    }

    @Override
    public BatchPriceResultVO calculateBatchPrice(BatchPriceCalculateRequest request) {
        // 获取模型基础价格
        SysModel model = modelRepository.selectById(request.getModelId());
        if (model == null) {
            throw new BusinessException("模型不存在");
        }

        BigDecimal basePrice = model.getBasePrice();

        // 获取材质价格
        BigDecimal materialCost = BigDecimal.ZERO;
        if (request.getMaterialId() != null) {
            ModelMaterial material = materialRepository.selectById(request.getMaterialId());
            if (material != null && material.getPrice() != null) {
                materialCost = BigDecimal.valueOf(material.getPrice());
            }
        }

        BigDecimal unitPrice = basePrice.add(materialCost);

        // 计算批量折扣
        SysBatchPrintDiscount discount = getApplicableBatchDiscount(request.getQuantity());
        BigDecimal discountPercent = discount != null ? discount.getDiscountPercent() : BigDecimal.valueOf(100);

        BigDecimal discountedUnitPrice = unitPrice.multiply(discountPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal totalAmount = discountedUnitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));
        BigDecimal originalTotal = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));
        BigDecimal savedAmount = originalTotal.subtract(totalAmount);

        return BatchPriceResultVO.builder()
                .originalUnitPrice(unitPrice)
                .discountedUnitPrice(discountedUnitPrice)
                .discountPercent(discountPercent)
                .quantity(request.getQuantity())
                .totalAmount(totalAmount)
                .savedAmount(savedAmount)
                .discountRule(discount != null ? discount.getDescription() : null)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrderForParticipant(Long participantId, Long userId) {
        // 获取参与记录
        SysGroupBuyParticipant participant = participantRepository.selectById(participantId);
        if (participant == null) {
            throw new BusinessException("参与记录不存在");
        }

        // 验证用户
        if (!participant.getUserId().equals(userId)) {
            throw new BusinessException("无权操作");
        }

        // 验证状态
        if (!ParticipantStatus.PENDING_PAYMENT.getCode().equals(participant.getStatus())) {
            throw new BusinessException("当前状态不支持创建订单");
        }

        // 检查是否已有订单
        if (participant.getOrderId() != null) {
            return participant.getOrderId();
        }

        // 获取活动信息
        SysGroupBuyActivity activity = activityRepository.selectById(participant.getActivityId());
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }

        // 创建订单
        SysOrder order = new SysOrder();
        order.setOrderSn(generateOrderSn());
        order.setUserId(userId);
        order.setModelId(activity.getModelId());
        order.setMaterialId(participant.getMaterialId());
        order.setOrderPrice(participant.getTotalAmount());
        order.setOrderStatus(0); // 待支付
        order.setGroupBuyGroupId(participant.getGroupId());
        order.setGroupBuyParticipantId(participantId);
        order.setBatchQuantity(participant.getQuantity());
        order.setBatchDiscount(participant.getUnitPrice().divide(activity.getOriginalPrice(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));

        // 构建定制参数
        Map<String, Object> customParams = new HashMap<>();
        customParams.put("color", participant.getColor());
        customParams.put("scale", participant.getScale());
        customParams.put("fillPercent", participant.getFillPercent());
        customParams.put("quantity", participant.getQuantity());
        if (participant.getCustomParams() != null) {
            customParams.put("extra", participant.getCustomParams());
        }
        try {
            order.setCustomParams(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(customParams));
        } catch (Exception e) {
            log.warn("序列化定制参数失败", e);
        }

        orderRepository.insert(order);

        // 更新参与记录的订单ID
        participant.setOrderId(order.getId());
        participant.setOrderSn(order.getOrderSn());
        participantRepository.updateById(participant);

        return order.getId();
    }

    private String generateOrderSn() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        return ORDER_SN_PREFIX + timestamp + random;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleParticipantPaid(Long participantId) {
        SysGroupBuyParticipant participant = participantRepository.selectById(participantId);
        if (participant == null) {
            return;
        }

        // 更新参与状态
        participant.setStatus(ParticipantStatus.PAID.getCode());
        participant.setPayTime(LocalDateTime.now());
        participantRepository.updateById(participant);

        // 检查拼团是否成功
        checkAndProcessGroupSuccess(participant.getGroupId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkAndProcessGroupSuccess(Long groupId) {
        SysGroupBuyGroup group = groupRepository.selectById(groupId);
        if (group == null || !GroupBuyStatus.IN_PROGRESS.getCode().equals(group.getStatus())) {
            return;
        }

        // 获取已支付人数
        Long paidCount = participantRepository.selectCount(
                new LambdaQueryWrapper<SysGroupBuyParticipant>()
                        .eq(SysGroupBuyParticipant::getGroupId, groupId)
                        .eq(SysGroupBuyParticipant::getStatus, ParticipantStatus.PAID.getCode())
        );

        // 检查是否达到目标人数
        if (paidCount >= group.getTargetPeople()) {
            // 更新拼团状态
            group.setStatus(GroupBuyStatus.SUCCESS.getCode());
            group.setSuccessTime(LocalDateTime.now());
            groupRepository.updateById(group);

            // 更新活动销量
            activityRepository.update(null, new LambdaUpdateWrapper<SysGroupBuyActivity>()
                    .eq(SysGroupBuyActivity::getId, group.getActivityId())
                    .setSql("sold_count = sold_count + " + paidCount.intValue())
            );

            log.info("拼团成功 groupId={}, paidCount={}", groupId, paidCount);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processTimeoutGroups() {
        // 查询已过期且进行中的拼团
        List<SysGroupBuyGroup> timeoutGroups = groupRepository.selectList(
                new LambdaQueryWrapper<SysGroupBuyGroup>()
                        .eq(SysGroupBuyGroup::getStatus, GroupBuyStatus.IN_PROGRESS.getCode())
                        .lt(SysGroupBuyGroup::getExpireTime, LocalDateTime.now())
        );

        for (SysGroupBuyGroup group : timeoutGroups) {
            try {
                processGroupFailure(group, "拼团超时");
            } catch (Exception ex) {
                log.error("处理拼团超时失败 groupId={}", group.getId(), ex);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processActivityStatus() {
        LocalDateTime now = LocalDateTime.now();

        // 结束已过期的活动
        activityRepository.update(null, new LambdaUpdateWrapper<SysGroupBuyActivity>()
                .eq(SysGroupBuyActivity::getStatus, 1)
                .lt(SysGroupBuyActivity::getEndTime, now)
                .set(SysGroupBuyActivity::getStatus, 2)
        );
    }

    // ========== 私有方法 ==========

    private SysGroupBuyActivity validateActivity(Long activityId) {
        SysGroupBuyActivity activity = activityRepository.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }

        if (activity.getStatus() != 1) {
            throw new BusinessException("活动未启用");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            throw new BusinessException("活动尚未开始");
        }
        if (now.isAfter(activity.getEndTime())) {
            throw new BusinessException("活动已结束");
        }

        return activity;
    }

    private void checkUserExistingGroup(Long userId, Long activityId) {
        // 查询用户在该活动中是否有进行中的参与记录
        List<SysGroupBuyParticipant> existingParticipants = participantRepository.selectList(
                new LambdaQueryWrapper<SysGroupBuyParticipant>()
                        .eq(SysGroupBuyParticipant::getUserId, userId)
                        .eq(SysGroupBuyParticipant::getActivityId, activityId)
                        .ne(SysGroupBuyParticipant::getStatus, ParticipantStatus.CANCELED.getCode())
                        .ne(SysGroupBuyParticipant::getStatus, ParticipantStatus.REFUNDED.getCode())
        );

        for (SysGroupBuyParticipant p : existingParticipants) {
            SysGroupBuyGroup group = groupRepository.selectById(p.getGroupId());
            if (group != null && GroupBuyStatus.IN_PROGRESS.getCode().equals(group.getStatus())) {
                throw new BusinessException("您已参与该活动的其他拼团，请先完成或取消");
            }
        }
    }

    private BigDecimal calculateUnitPrice(SysGroupBuyActivity activity, Integer quantity) {
        BigDecimal basePrice = activity.getGroupPrice();

        // 批量打印折扣
        SysBatchPrintDiscount batchDiscount = getApplicableBatchDiscount(quantity);
        BigDecimal batchDiscountPercent = batchDiscount != null
                ? batchDiscount.getDiscountPercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                : BigDecimal.ONE;

        // 活动折扣
        BigDecimal activityDiscountPercent = BigDecimal.ONE;
        if (GroupBuyDiscountType.FIXED.getCode().equals(activity.getDiscountType())) {
            activityDiscountPercent = activity.getDiscountValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        }

        return basePrice.multiply(batchDiscountPercent)
                .multiply(activityDiscountPercent)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private SysBatchPrintDiscount getApplicableBatchDiscount(Integer quantity) {
        List<SysBatchPrintDiscount> discounts = batchDiscountRepository.selectList(
                new LambdaQueryWrapper<SysBatchPrintDiscount>()
                        .eq(SysBatchPrintDiscount::getIsActive, 1)
                        .le(SysBatchPrintDiscount::getMinQuantity, quantity)
                        .and(wrapper -> wrapper
                                .isNull(SysBatchPrintDiscount::getMaxQuantity)
                                .or()
                                .ge(SysBatchPrintDiscount::getMaxQuantity, quantity)
                        )
                        .orderByDesc(SysBatchPrintDiscount::getMinQuantity)
                        .last("LIMIT 1")
        );

        return discounts.isEmpty() ? null : discounts.get(0);
    }

    private SysGroupBuyParticipant createParticipant(
            SysGroupBuyGroup group,
            SysGroupBuyActivity activity,
            Long userId,
            GroupBuyCreateRequest request,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            boolean isLeader
    ) {
        SysGroupBuyParticipant participant = new SysGroupBuyParticipant();
        participant.setGroupId(group.getId());
        participant.setActivityId(activity.getId());
        participant.setUserId(userId);
        participant.setIsLeader(isLeader ? 1 : 0);
        participant.setQuantity(request.getQuantity());
        participant.setUnitPrice(unitPrice);
        participant.setTotalAmount(totalAmount);
        participant.setMaterialId(request.getMaterialId());
        participant.setColor(request.getColor());
        participant.setScale(request.getScale());
        participant.setFillPercent(request.getFillPercent());
        // 保存额外定制参数
        if (StringUtils.hasText(request.getCustomParams())) {
            participant.setCustomParams(request.getCustomParams());
        } else if (StringUtils.hasText(request.getNote()) || request.getPrecision() != null || request.getFilamentDiameter() != null) {
            // 构建定制参数JSON
            Map<String, Object> params = new HashMap<>();
            if (StringUtils.hasText(request.getNote())) {
                params.put("note", request.getNote());
            }
            if (request.getPrecision() != null) {
                params.put("precision", request.getPrecision());
            }
            if (request.getFilamentDiameter() != null) {
                params.put("filamentDiameter", request.getFilamentDiameter());
            }
            try {
                participant.setCustomParams(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(params));
            } catch (Exception e) {
                log.warn("序列化定制参数失败", e);
            }
        }
        participant.setStatus(ParticipantStatus.PENDING_PAYMENT.getCode());
        participant.setIsDelete(0);
        participantRepository.insert(participant);
        return participant;
    }

    private SysGroupBuyParticipant createParticipant(
            SysGroupBuyGroup group,
            SysGroupBuyActivity activity,
            Long userId,
            GroupBuyJoinRequest request,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            boolean isLeader
    ) {
        SysGroupBuyParticipant participant = new SysGroupBuyParticipant();
        participant.setGroupId(group.getId());
        participant.setActivityId(activity.getId());
        participant.setUserId(userId);
        participant.setIsLeader(isLeader ? 1 : 0);
        participant.setQuantity(request.getQuantity());
        participant.setUnitPrice(unitPrice);
        participant.setTotalAmount(totalAmount);
        participant.setMaterialId(request.getMaterialId());
        participant.setColor(request.getColor());
        participant.setScale(request.getScale());
        participant.setFillPercent(request.getFillPercent());
        // 保存额外定制参数
        if (StringUtils.hasText(request.getCustomParams())) {
            participant.setCustomParams(request.getCustomParams());
        } else if (StringUtils.hasText(request.getNote()) || request.getPrecision() != null || request.getFilamentDiameter() != null) {
            Map<String, Object> params = new HashMap<>();
            if (StringUtils.hasText(request.getNote())) {
                params.put("note", request.getNote());
            }
            if (request.getPrecision() != null) {
                params.put("precision", request.getPrecision());
            }
            if (request.getFilamentDiameter() != null) {
                params.put("filamentDiameter", request.getFilamentDiameter());
            }
            try {
                participant.setCustomParams(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(params));
            } catch (Exception e) {
                log.warn("序列化定制参数失败", e);
            }
        }
        participant.setStatus(ParticipantStatus.PENDING_PAYMENT.getCode());
        participant.setIsDelete(0);
        participantRepository.insert(participant);
        return participant;
    }

    private void processGroupFailure(SysGroupBuyGroup group, String reason) {
        // 更新拼团状态
        group.setStatus(GroupBuyStatus.FAILED.getCode());
        group.setFailTime(LocalDateTime.now());
        group.setFailReason(reason);
        groupRepository.updateById(group);

        // 更新所有待支付的参与记录为已取消
        participantRepository.update(null, new LambdaUpdateWrapper<SysGroupBuyParticipant>()
                .eq(SysGroupBuyParticipant::getGroupId, group.getId())
                .eq(SysGroupBuyParticipant::getStatus, ParticipantStatus.PENDING_PAYMENT.getCode())
                .set(SysGroupBuyParticipant::getStatus, ParticipantStatus.CANCELED.getCode())
        );

        log.info("拼团失败 groupId={}, reason={}", group.getId(), reason);
    }

    private String generateGroupSn() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return GROUP_SN_PREFIX + date + random;
    }

    private String generateShareCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < SHARE_CODE_LENGTH; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private GroupBuyActivityVO toActivityVO(SysGroupBuyActivity activity) {
        GroupBuyActivityVO vo = new GroupBuyActivityVO();
        vo.setId(activity.getId());
        vo.setActivityName(activity.getActivityName());
        vo.setModelId(activity.getModelId());
        vo.setOriginalPrice(activity.getOriginalPrice());
        vo.setGroupPrice(activity.getGroupPrice());
        vo.setMinPeople(activity.getMinPeople());
        vo.setMaxPeople(activity.getMaxPeople());
        vo.setDiscountType(activity.getDiscountType());
        vo.setDiscountValue(activity.getDiscountValue());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setTimeoutHours(activity.getTimeoutHours());
        vo.setSoldCount(activity.getSoldCount());
        vo.setTotalStock(activity.getTotalStock());
        vo.setCoverImage(activity.getCoverImage());
        vo.setStatus(activity.getStatus());

        // 获取模型信息
        SysModel model = modelRepository.selectById(activity.getModelId());
        if (model != null) {
            vo.setModelName(model.getModelName());
            // 获取主图
            SysModelImage mainImage = modelImageRepository.selectOne(
                    new LambdaQueryWrapper<SysModelImage>()
                            .eq(SysModelImage::getModelId, model.getId())
                            .eq(SysModelImage::getIsMain, 1)
                            .last("LIMIT 1")
            );
            if (mainImage != null) {
                vo.setModelImage(mainImage.getImageUrl());
            }
        }

        return vo;
    }

    private GroupBuyActivityDetailVO toActivityDetailVO(SysGroupBuyActivity activity) {
        GroupBuyActivityDetailVO vo = new GroupBuyActivityDetailVO();
        vo.setId(activity.getId());
        vo.setActivityName(activity.getActivityName());
        vo.setModelId(activity.getModelId());
        vo.setOriginalPrice(activity.getOriginalPrice());
        vo.setGroupPrice(activity.getGroupPrice());
        vo.setMinPeople(activity.getMinPeople());
        vo.setMaxPeople(activity.getMaxPeople());
        vo.setDiscountType(activity.getDiscountType());
        vo.setDiscountValue(activity.getDiscountValue());
        vo.setLadderConfig(activity.getLadderConfig());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setTimeoutHours(activity.getTimeoutHours());
        vo.setSoldCount(activity.getSoldCount());
        vo.setTotalStock(activity.getTotalStock());
        vo.setCoverImage(activity.getCoverImage());
        vo.setDescription(activity.getDescription());
        vo.setStatus(activity.getStatus());

        // 获取模型信息
        SysModel model = modelRepository.selectById(activity.getModelId());
        if (model != null) {
            vo.setModelName(model.getModelName());
            vo.setModelDescription(model.getDescription());
            // 获取主图
            SysModelImage mainImage = modelImageRepository.selectOne(
                    new LambdaQueryWrapper<SysModelImage>()
                            .eq(SysModelImage::getModelId, model.getId())
                            .eq(SysModelImage::getIsMain, 1)
                            .last("LIMIT 1")
            );
            if (mainImage != null) {
                vo.setModelImage(mainImage.getImageUrl());
            }

            // 获取模型材质列表
            List<ModelMaterial> modelMaterials = materialRepository.selectList(
                    new LambdaQueryWrapper<ModelMaterial>()
                            .eq(ModelMaterial::getModelId, model.getId())
            );
            if (modelMaterials != null && !modelMaterials.isEmpty()) {
                List<MaterialVO> materialVOs = modelMaterials.stream()
                        .map(m -> MaterialVO.builder()
                                .id(m.getId())
                                .name(m.getMaterialName())
                                .price(m.getPrice() != null ? BigDecimal.valueOf(m.getPrice()) : BigDecimal.ZERO)
                                .isTrusted(m.getIsTrusted())
                                .isEco(m.getIsEco())
                                .build())
                        .collect(Collectors.toList());
                vo.setMaterials(materialVOs);
            }
        }

        return vo;
    }

    private GroupBuyGroupVO toGroupVO(SysGroupBuyGroup group) {
        GroupBuyGroupVO vo = new GroupBuyGroupVO();
        vo.setId(group.getId());
        vo.setActivityId(group.getActivityId());
        vo.setGroupSn(group.getGroupSn());
        vo.setCurrentPeople(group.getCurrentPeople());
        vo.setTargetPeople(group.getTargetPeople());
        vo.setStatus(group.getStatus());
        vo.setExpireTime(group.getExpireTime());
        vo.setLeaderUserId(group.getLeaderUserId());

        // 计算剩余时间
        if (GroupBuyStatus.IN_PROGRESS.getCode().equals(group.getStatus())) {
            long remainingSeconds = java.time.Duration.between(
                    LocalDateTime.now(), group.getExpireTime()
            ).getSeconds();
            vo.setRemainingSeconds(Math.max(0, remainingSeconds));
        }

        // 获取活动信息
        SysGroupBuyActivity activity = activityRepository.selectById(group.getActivityId());
        if (activity != null) {
            vo.setActivityName(activity.getActivityName());
            vo.setGroupPrice(activity.getGroupPrice());

            SysModel model = modelRepository.selectById(activity.getModelId());
            if (model != null) {
                vo.setModelName(model.getModelName());
                SysModelImage mainImage = modelImageRepository.selectOne(
                        new LambdaQueryWrapper<SysModelImage>()
                                .eq(SysModelImage::getModelId, model.getId())
                                .eq(SysModelImage::getIsMain, 1)
                                .last("LIMIT 1")
                );
                if (mainImage != null) {
                    vo.setModelImage(mainImage.getImageUrl());
                }
            }
        }

        // 获取团长信息
        SysUser leader = userRepository.selectById(group.getLeaderUserId());
        if (leader != null) {
            vo.setLeaderNickname(leader.getNickname());
            vo.setLeaderAvatar(leader.getAvatar());
        }

        return vo;
    }

    private GroupBuyGroupDetailVO buildGroupDetailVO(SysGroupBuyGroup group, Long currentUserId) {
        GroupBuyGroupDetailVO vo = new GroupBuyGroupDetailVO();
        vo.setId(group.getId());
        vo.setActivityId(group.getActivityId());
        vo.setGroupSn(group.getGroupSn());
        vo.setShareCode(group.getShareCode());
        vo.setCurrentPeople(group.getCurrentPeople());
        vo.setTargetPeople(group.getTargetPeople());
        vo.setStatus(group.getStatus());
        vo.setExpireTime(group.getExpireTime());

        // 计算剩余时间
        if (GroupBuyStatus.IN_PROGRESS.getCode().equals(group.getStatus())) {
            long remainingSeconds = java.time.Duration.between(
                    LocalDateTime.now(), group.getExpireTime()
            ).getSeconds();
            vo.setRemainingSeconds(Math.max(0, remainingSeconds));
        }

        // 获取活动信息
        SysGroupBuyActivity activity = activityRepository.selectById(group.getActivityId());
        if (activity != null) {
            vo.setActivity(toActivityVO(activity));
        }

        // 获取参与成员
        List<SysGroupBuyParticipant> participants = participantRepository.selectList(
                new LambdaQueryWrapper<SysGroupBuyParticipant>()
                        .eq(SysGroupBuyParticipant::getGroupId, group.getId())
                        .ne(SysGroupBuyParticipant::getStatus, ParticipantStatus.CANCELED.getCode())
                        .orderByDesc(SysGroupBuyParticipant::getIsLeader)
                        .orderByAsc(SysGroupBuyParticipant::getCreateTime)
        );

        List<GroupBuyParticipantVO> participantVOs = new ArrayList<>();
        GroupBuyParticipantVO myParticipant = null;

        for (SysGroupBuyParticipant p : participants) {
            GroupBuyParticipantVO pvo = toParticipantVO(p);

            if (p.getIsLeader() == 1) {
                vo.setLeader(pvo);
            }
            participantVOs.add(pvo);

            if (currentUserId != null && p.getUserId().equals(currentUserId)) {
                vo.setHasJoined(true);
                myParticipant = pvo;
            }
        }

        vo.setParticipants(participantVOs);
        vo.setMyParticipant(myParticipant);

        if (vo.getHasJoined() == null) {
            vo.setHasJoined(false);
        }

        return vo;
    }

    private GroupBuyParticipantVO toParticipantVO(SysGroupBuyParticipant participant) {
        GroupBuyParticipantVO vo = new GroupBuyParticipantVO();
        vo.setId(participant.getId());
        vo.setUserId(participant.getUserId());
        vo.setIsLeader(participant.getIsLeader() == 1);
        vo.setQuantity(participant.getQuantity());
        vo.setUnitPrice(participant.getUnitPrice());
        vo.setTotalAmount(participant.getTotalAmount());
        vo.setMaterialId(participant.getMaterialId());
        vo.setColor(participant.getColor());
        vo.setStatus(participant.getStatus());
        vo.setPayTime(participant.getPayTime());
        vo.setCreateTime(participant.getCreateTime());
        vo.setOrderId(participant.getOrderId());
        vo.setOrderSn(participant.getOrderSn());

        // 获取用户信息
        SysUser user = userRepository.selectById(participant.getUserId());
        if (user != null) {
            vo.setUserNickname(user.getNickname());
            vo.setUserAvatar(user.getAvatar());
        }

        // 获取材质名称
        if (participant.getMaterialId() != null) {
            ModelMaterial material = materialRepository.selectById(participant.getMaterialId());
            if (material != null) {
                vo.setMaterialName(material.getMaterialName());
            }
        }

        return vo;
    }
}
