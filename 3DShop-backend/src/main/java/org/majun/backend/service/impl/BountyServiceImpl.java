package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.dto.BountyAcceptRequest;
import org.majun.backend.dto.BountyBidCreateRequest;
import org.majun.backend.dto.BountyBidUpdateRequest;
import org.majun.backend.dto.BountyDeliverySubmitRequest;
import org.majun.backend.dto.BountyMessageSendRequest;
import org.majun.backend.dto.BountyPickBidRequest;
import org.majun.backend.dto.BountyPriceChangeConfirmRequest;
import org.majun.backend.dto.BountyPriceChangeRequest;
import org.majun.backend.dto.BountyTaskCreateRequest;
import org.majun.backend.dto.BountyTaskQueryRequest;
import org.majun.backend.dto.BountyTaskReviewRequest;
import org.majun.backend.entity.BountyBid;
import org.majun.backend.entity.BountyDelivery;
import org.majun.backend.entity.BountyEscrow;
import org.majun.backend.entity.BountyMessage;
import org.majun.backend.entity.BountyPriceChange;
import org.majun.backend.entity.BountyStatusLog;
import org.majun.backend.entity.BountyTask;
import org.majun.backend.entity.BountyTaskAttachment;
import org.majun.backend.enums.BountyBidStatus;
import org.majun.backend.enums.BountyDeliveryStatus;
import org.majun.backend.enums.BountyEscrowStatus;
import org.majun.backend.enums.BountyPriceChangeStatus;
import org.majun.backend.enums.BountyTaskStatus;
import org.majun.backend.repository.BountyBidRepository;
import org.majun.backend.repository.BountyDeliveryRepository;
import org.majun.backend.repository.BountyEscrowRepository;
import org.majun.backend.repository.BountyMessageRepository;
import org.majun.backend.repository.BountyPriceChangeRepository;
import org.majun.backend.repository.BountyStatusLogRepository;
import org.majun.backend.repository.BountyTaskRepository;
import org.majun.backend.repository.BountyTaskAttachmentRepository;
import org.majun.backend.service.BountyService;
import org.majun.backend.service.BountyWebSocketService;
import org.majun.backend.vo.BountyBidVO;
import org.majun.backend.vo.BountyMessageVO;
import org.majun.backend.vo.BountyTaskDetailVO;
import org.majun.backend.vo.BountyTaskListVO;
import org.majun.backend.vo.PageResult;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BountyServiceImpl implements BountyService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BountyTaskRepository bountyTaskRepository;
    private final BountyBidRepository bountyBidRepository;
    private final BountyDeliveryRepository bountyDeliveryRepository;
    private final BountyEscrowRepository bountyEscrowRepository;
    private final BountyPriceChangeRepository bountyPriceChangeRepository;
    private final BountyMessageRepository bountyMessageRepository;
    private final BountyStatusLogRepository bountyStatusLogRepository;
    private final BountyWebSocketService bountyWebSocketService;
    private final BountyFinanceService bountyFinanceService;
    private final BountyTaskAttachmentRepository bountyTaskAttachmentRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTask(BountyTaskCreateRequest request, Long userId) {
        BountyTask task = new BountyTask();
        task.setTaskSn(buildTaskSn());
        task.setPublisherId(userId);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setCategory(request.getCategory());
        task.setTags(request.getTags());
        task.setBudgetAmount(request.getBudgetAmount());
        task.setFinalAmount(request.getBudgetAmount());
        task.setExpectedDays(request.getExpectedDays());
        task.setDeadlineTime(parseDateTime(request.getDeadlineTime()));
        task.setStatus(BountyTaskStatus.RECRUITING.getCode());
        task.setVersion(0);
        bountyTaskRepository.insert(task);

        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            int sortNo = 0;
            for (String attachmentUrl : request.getAttachments()) {
                if (!StringUtils.hasText(attachmentUrl)) {
                    continue;
                }
                BountyTaskAttachment attachment = new BountyTaskAttachment();
                attachment.setTaskId(task.getId());
                attachment.setFileUrl(attachmentUrl.trim());
                attachment.setSortNo(sortNo++);
                attachment.setIsDelete(0);
                bountyTaskAttachmentRepository.insert(attachment);
            }
        }

        BountyEscrow escrow = new BountyEscrow();
        escrow.setTaskId(task.getId());
        escrow.setPayerId(userId);
        escrow.setEscrowAmount(task.getBudgetAmount());
        escrow.setReleasedAmount(BigDecimal.ZERO);
        escrow.setRefundAmount(BigDecimal.ZERO);
        escrow.setStatus(BountyEscrowStatus.ESCROWED.getCode());
        escrow.setVersion(0);
        bountyEscrowRepository.insert(escrow);

        appendStatusLog(task.getId(), null, task.getStatus(), userId, "USER", "创建悬赏任务");
        return task.getId();
    }

    @Override
    public PageResult<BountyTaskListVO> pageTasks(BountyTaskQueryRequest request, Long userId) {
        Page<BountyTask> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<BountyTask> wrapper = new LambdaQueryWrapper<BountyTask>()
                .eq(BountyTask::getIsDelete, 0)
                .orderByDesc(BountyTask::getCreateTime);

        if (request.getStatus() != null) {
            wrapper.eq(BountyTask::getStatus, request.getStatus());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(BountyTask::getTitle, request.getKeyword().trim());
        }
        if (Boolean.TRUE.equals(request.getOnlyMine())) {
            wrapper.eq(BountyTask::getPublisherId, userId);
        }

        Page<BountyTask> result = bountyTaskRepository.selectPage(page, wrapper);
        List<BountyTaskListVO> records = result.getRecords().stream().map(this::toListVO).collect(Collectors.toList());

        return PageResult.<BountyTaskListVO>builder()
                .records(records)
                .total(result.getTotal())
                .pageNum(request.getPageNum())
                .pageSize(request.getPageSize())
                .pages((int) result.getPages())
                .build();
    }

    @Override
    public BountyTaskDetailVO getTaskDetail(Long taskId, Long userId) {
        BountyTask task = getTaskOrThrow(taskId);
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        return buildTaskDetail(task);
    }

    @Override
    public BountyTaskDetailVO getTaskDetailForAdmin(Long taskId) {
        BountyTask task = getTaskOrThrow(taskId);
        return buildTaskDetail(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewTask(BountyTaskReviewRequest request, Long adminId) {
        BountyTask task = getTaskOrThrow(request.getTaskId());
        Integer beforeStatus = task.getStatus();

        if (Objects.equals(request.getDecision(), 1)) {
            if (Objects.equals(task.getStatus(), BountyTaskStatus.CLOSED.getCode())) {
                throw new BusinessException("已关闭任务不可审核通过");
            }
            if (!Objects.equals(task.getStatus(), BountyTaskStatus.RECRUITING.getCode())) {
                task.setStatus(BountyTaskStatus.RECRUITING.getCode());
            }
            task.setCloseReason(null);
            bountyTaskRepository.updateById(task);
            appendStatusLog(task.getId(), beforeStatus, task.getStatus(), adminId, "ADMIN",
                    StringUtils.hasText(request.getRemark()) ? request.getRemark() : "管理员审核通过");
            sendSystemMessage(task.getId(), "任务已通过平台审核");
            return;
        }

        if (Objects.equals(request.getDecision(), 2)) {
            task.setStatus(BountyTaskStatus.CLOSED.getCode());
            task.setCloseReason(StringUtils.hasText(request.getRemark()) ? request.getRemark() : "平台审核驳回");
            bountyTaskRepository.updateById(task);
            appendStatusLog(task.getId(), beforeStatus, task.getStatus(), adminId, "ADMIN", task.getCloseReason());
            sendSystemMessage(task.getId(), "任务已被平台审核驳回");
            return;
        }

        throw new BusinessException("审核结论仅支持1或2");
    }

    private BountyTaskDetailVO buildTaskDetail(BountyTask task) {
        Long taskId = task.getId();
        BountyTaskDetailVO vo = new BountyTaskDetailVO();
        BeanUtils.copyProperties(task, vo);

        List<BountyBid> bids = bountyBidRepository.selectList(new LambdaQueryWrapper<BountyBid>()
                .eq(BountyBid::getTaskId, taskId)
                .eq(BountyBid::getIsDelete, 0)
                .orderByDesc(BountyBid::getCreateTime));
        vo.setBids(bids.stream().map(this::toBidVO).collect(Collectors.toList()));

        List<BountyTaskAttachment> attachments = bountyTaskAttachmentRepository.selectList(new LambdaQueryWrapper<BountyTaskAttachment>()
            .eq(BountyTaskAttachment::getTaskId, taskId)
            .eq(BountyTaskAttachment::getIsDelete, 0)
            .orderByAsc(BountyTaskAttachment::getSortNo)
            .orderByAsc(BountyTaskAttachment::getId));
        vo.setAttachments(attachments.stream().map(BountyTaskAttachment::getFileUrl).filter(StringUtils::hasText).collect(Collectors.toList()));

        BountyDelivery pendingDelivery = bountyDeliveryRepository.selectOne(new LambdaQueryWrapper<BountyDelivery>()
            .eq(BountyDelivery::getTaskId, taskId)
            .eq(BountyDelivery::getStatus, BountyDeliveryStatus.SUBMITTED.getCode())
            .eq(BountyDelivery::getIsDelete, 0)
            .orderByDesc(BountyDelivery::getDeliveryRound)
            .orderByDesc(BountyDelivery::getCreateTime)
            .last("limit 1"));
        vo.setPendingDeliveryId(pendingDelivery == null ? null : pendingDelivery.getId());

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBid(BountyBidCreateRequest request, Long userId) {
        BountyTask task = getTaskOrThrow(request.getTaskId());
        if (!Objects.equals(task.getStatus(), BountyTaskStatus.RECRUITING.getCode())) {
            throw new BusinessException("当前状态不可竞标");
        }
        if (Objects.equals(task.getPublisherId(), userId)) {
            throw new BusinessException("发布者不能竞标自己的任务");
        }

        BountyBid existed = bountyBidRepository.selectOne(new LambdaQueryWrapper<BountyBid>()
                .eq(BountyBid::getTaskId, request.getTaskId())
                .eq(BountyBid::getDesignerId, userId)
                .eq(BountyBid::getIsDelete, 0)
                .last("limit 1"));
        if (existed != null) {
            if (!Objects.equals(existed.getStatus(), BountyBidStatus.WITHDRAWN.getCode())) {
                throw new BusinessException("你已参与该任务竞标");
            }

            existed.setQuoteAmount(request.getQuoteAmount());
            existed.setDeliveryDays(request.getDeliveryDays());
            existed.setProposal(request.getProposal());
            existed.setStatus(BountyBidStatus.SUBMITTED.getCode());
            bountyBidRepository.updateById(existed);

            sendSystemMessage(task.getId(), "有新竞标方案，请及时查看");
            return existed.getId();
        }

        BountyBid bid = new BountyBid();
        bid.setTaskId(request.getTaskId());
        bid.setDesignerId(userId);
        bid.setQuoteAmount(request.getQuoteAmount());
        bid.setDeliveryDays(request.getDeliveryDays());
        bid.setProposal(request.getProposal());
        bid.setStatus(BountyBidStatus.SUBMITTED.getCode());
        bountyBidRepository.insert(bid);

        sendSystemMessage(task.getId(), "有新竞标方案，请及时查看");
        return bid.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBid(BountyBidUpdateRequest request, Long userId) {
        BountyBid bid = bountyBidRepository.selectById(request.getBidId());
        if (bid == null || Objects.equals(bid.getIsDelete(), 1)) {
            throw new BusinessException("竞标记录不存在");
        }
        if (!Objects.equals(bid.getDesignerId(), userId)) {
            throw new BusinessException("仅可修改自己的竞标");
        }
        if (!Objects.equals(bid.getStatus(), BountyBidStatus.SUBMITTED.getCode())) {
            throw new BusinessException("当前竞标状态不可修改");
        }

        BountyTask task = getTaskOrThrow(bid.getTaskId());
        if (!Objects.equals(task.getStatus(), BountyTaskStatus.RECRUITING.getCode())) {
            throw new BusinessException("任务当前状态不可修改竞标");
        }

        bid.setQuoteAmount(request.getQuoteAmount());
        bid.setDeliveryDays(request.getDeliveryDays());
        bid.setProposal(request.getProposal());
        bountyBidRepository.updateById(bid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawBid(Long bidId, Long userId) {
        BountyBid bid = bountyBidRepository.selectById(bidId);
        if (bid == null || Objects.equals(bid.getIsDelete(), 1)) {
            throw new BusinessException("竞标记录不存在");
        }
        if (!Objects.equals(bid.getDesignerId(), userId)) {
            throw new BusinessException("仅可撤回自己的竞标");
        }
        if (!Objects.equals(bid.getStatus(), BountyBidStatus.SUBMITTED.getCode())) {
            throw new BusinessException("当前竞标状态不可撤回");
        }

        BountyTask task = getTaskOrThrow(bid.getTaskId());
        if (!Objects.equals(task.getStatus(), BountyTaskStatus.RECRUITING.getCode())) {
            throw new BusinessException("任务当前状态不可撤回竞标");
        }

        bid.setStatus(BountyBidStatus.WITHDRAWN.getCode());
        bountyBidRepository.updateById(bid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pickBid(BountyPickBidRequest request, Long userId) {
        BountyTask task = getTaskOrThrow(request.getTaskId());
        if (!Objects.equals(task.getPublisherId(), userId)) {
            throw new BusinessException("仅发布者可选标");
        }
        if (!Objects.equals(task.getStatus(), BountyTaskStatus.RECRUITING.getCode())) {
            throw new BusinessException("当前任务状态不可选标");
        }

        BountyBid bid = bountyBidRepository.selectById(request.getBidId());
        if (bid == null || !Objects.equals(bid.getTaskId(), task.getId()) || Objects.equals(bid.getIsDelete(), 1)) {
            throw new BusinessException("竞标记录不存在");
        }

        bountyBidRepository.update(null, new LambdaUpdateWrapper<BountyBid>()
                .eq(BountyBid::getTaskId, task.getId())
                .eq(BountyBid::getIsDelete, 0)
                .set(BountyBid::getStatus, BountyBidStatus.LOST.getCode()));

        bid.setStatus(BountyBidStatus.WINNER.getCode());
        bountyBidRepository.updateById(bid);

        Integer beforeStatus = task.getStatus();
        task.setWinnerBidId(bid.getId());
        task.setWinnerDesignerId(bid.getDesignerId());
        task.setFinalAmount(bid.getQuoteAmount());
        task.setStatus(BountyTaskStatus.PICKED.getCode());
        bountyTaskRepository.updateById(task);

        appendStatusLog(task.getId(), beforeStatus, task.getStatus(), userId, "USER", request.getPickReason());
        sendSystemMessage(task.getId(), "任务已完成选标，请中标设计者开始交付");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitDelivery(BountyDeliverySubmitRequest request, Long userId) {
        BountyTask task = getTaskOrThrow(request.getTaskId());
        if (!Objects.equals(task.getWinnerDesignerId(), userId)) {
            throw new BusinessException("仅中标设计者可提交交付");
        }
        if (!Objects.equals(task.getStatus(), BountyTaskStatus.PICKED.getCode())
                && !Objects.equals(task.getStatus(), BountyTaskStatus.IN_DELIVERY.getCode())) {
            throw new BusinessException("当前任务状态不可提交交付");
        }

        long round = bountyDeliveryRepository.selectCount(new LambdaQueryWrapper<BountyDelivery>()
                .eq(BountyDelivery::getTaskId, task.getId())
                .eq(BountyDelivery::getIsDelete, 0)) + 1;

        BountyDelivery delivery = new BountyDelivery();
        delivery.setTaskId(task.getId());
        delivery.setBidId(task.getWinnerBidId());
        delivery.setDesignerId(userId);
        delivery.setDeliveryRound((int) round);
        delivery.setDescription(request.getDescription());
        delivery.setStatus(BountyDeliveryStatus.SUBMITTED.getCode());
        delivery.setIsFinal(request.getIsFinal() == null ? 1 : request.getIsFinal());
        bountyDeliveryRepository.insert(delivery);

        Integer beforeStatus = task.getStatus();
        task.setStatus(BountyTaskStatus.WAIT_ACCEPTANCE.getCode());
        bountyTaskRepository.updateById(task);
        appendStatusLog(task.getId(), beforeStatus, task.getStatus(), userId, "DESIGNER", "提交交付");

        sendSystemMessage(task.getId(), "有新的交付成果，请发布者进行验收");
        return delivery.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptDelivery(BountyAcceptRequest request, Long userId) {
        BountyTask task = getTaskOrThrow(request.getTaskId());
        if (!Objects.equals(task.getPublisherId(), userId)) {
            throw new BusinessException("仅发布者可验收");
        }
        if (!Objects.equals(task.getStatus(), BountyTaskStatus.WAIT_ACCEPTANCE.getCode())) {
            throw new BusinessException("当前任务状态不可验收");
        }

        BountyDelivery delivery = bountyDeliveryRepository.selectById(request.getDeliveryId());
        if (delivery == null || !Objects.equals(delivery.getTaskId(), task.getId()) || Objects.equals(delivery.getIsDelete(), 1)) {
            throw new BusinessException("交付记录不存在");
        }

        Integer beforeStatus = task.getStatus();
        if (Objects.equals(request.getDecision(), 1)) {
            delivery.setStatus(BountyDeliveryStatus.ACCEPTED.getCode());
            task.setStatus(BountyTaskStatus.COMPLETED.getCode());

            bountyFinanceService.releaseToWinner(task);
            appendStatusLog(task.getId(), beforeStatus, task.getStatus(), userId, "USER", "验收通过");
            sendSystemMessage(task.getId(), "任务验收通过，资金已结算至设计者钱包");
        } else if (Objects.equals(request.getDecision(), 2)) {
            delivery.setStatus(BountyDeliveryStatus.NEED_REWORK.getCode());
            task.setStatus(BountyTaskStatus.IN_DELIVERY.getCode());
            appendStatusLog(task.getId(), beforeStatus, task.getStatus(), userId, "USER", "验收驳回");
            sendSystemMessage(task.getId(), "交付被驳回，请设计者修改后重新提交");
        } else {
            throw new BusinessException("验收决策仅支持1或2");
        }

        bountyDeliveryRepository.updateById(delivery);
        bountyTaskRepository.updateById(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long applyPriceChange(BountyPriceChangeRequest request, Long userId) {
        BountyTask task = getTaskOrThrow(request.getTaskId());
        if (!Objects.equals(task.getStatus(), BountyTaskStatus.PICKED.getCode())
                && !Objects.equals(task.getStatus(), BountyTaskStatus.IN_DELIVERY.getCode())) {
            throw new BusinessException("当前状态不支持发起改价");
        }
        boolean canApply = Objects.equals(task.getPublisherId(), userId) || Objects.equals(task.getWinnerDesignerId(), userId);
        if (!canApply) {
            throw new BusinessException("仅发布者和中标设计者可发起改价");
        }

        BountyPriceChange pending = bountyPriceChangeRepository.selectOne(new LambdaQueryWrapper<BountyPriceChange>()
                .eq(BountyPriceChange::getTaskId, task.getId())
                .eq(BountyPriceChange::getStatus, BountyPriceChangeStatus.PENDING.getCode())
                .eq(BountyPriceChange::getIsDelete, 0)
                .last("limit 1"));
        if (pending != null) {
            throw new BusinessException("当前有待确认的改价申请");
        }

        BountyPriceChange change = new BountyPriceChange();
        change.setTaskId(task.getId());
        change.setApplyBy(userId);
        change.setCurrentAmount(task.getFinalAmount());
        change.setTargetAmount(request.getTargetAmount());
        change.setReason(request.getReason());
        change.setStatus(BountyPriceChangeStatus.PENDING.getCode());
        change.setExpireTime(LocalDateTime.now().plusDays(2));
        bountyPriceChangeRepository.insert(change);

        sendSystemMessage(task.getId(), "有新的改价协商请求，请尽快处理");
        return change.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPriceChange(BountyPriceChangeConfirmRequest request, Long userId) {
        BountyPriceChange change = bountyPriceChangeRepository.selectById(request.getPriceChangeId());
        if (change == null || Objects.equals(change.getIsDelete(), 1)) {
            throw new BusinessException("改价记录不存在");
        }
        if (!Objects.equals(change.getStatus(), BountyPriceChangeStatus.PENDING.getCode())) {
            throw new BusinessException("当前改价记录不可操作");
        }

        BountyTask task = getTaskOrThrow(change.getTaskId());
        boolean isAnotherParty = !Objects.equals(change.getApplyBy(), userId)
                && (Objects.equals(task.getPublisherId(), userId) || Objects.equals(task.getWinnerDesignerId(), userId));
        if (!isAnotherParty) {
            throw new BusinessException("仅协商对方可确认改价");
        }

        change.setConfirmBy(userId);
        change.setConfirmTime(LocalDateTime.now());

        if (Objects.equals(request.getDecision(), 1)) {
            change.setStatus(BountyPriceChangeStatus.AGREED.getCode());
            BigDecimal previousAmount = task.getFinalAmount();
            change.setCurrentAmount(previousAmount);

            BigDecimal delta = change.getTargetAmount().subtract(previousAmount);
            if (delta.compareTo(BigDecimal.ZERO) > 0
                    && !bountyFinanceService.hasAvailableBalance(task.getPublisherId(), delta)) {
                preparePriceIncreasePendingPayment(task, change);
                sendSystemMessage(task.getId(), "改价已确认，发布者余额不足，已生成补差待支付单");
            } else {
                task.setFinalAmount(change.getTargetAmount());
                bountyTaskRepository.updateById(task);
                bountyFinanceService.settlePriceChange(task, change);
                sendSystemMessage(task.getId(), "改价已达成一致，任务金额已更新");
            }
        } else if (Objects.equals(request.getDecision(), 2)) {
            change.setStatus(BountyPriceChangeStatus.REJECTED.getCode());
            sendSystemMessage(task.getId(), "改价申请已被拒绝");
        } else {
            throw new BusinessException("确认决策仅支持1或2");
        }

        bountyPriceChangeRepository.updateById(change);
    }

    private void preparePriceIncreasePendingPayment(BountyTask task, BountyPriceChange change) {
        BountyEscrow escrow = bountyEscrowRepository.selectOne(new LambdaQueryWrapper<BountyEscrow>()
                .eq(BountyEscrow::getTaskId, task.getId())
                .eq(BountyEscrow::getIsDelete, 0)
                .last("limit 1"));
        if (escrow == null) {
            throw new BusinessException("托管记录不存在，无法生成补差支付单");
        }
        if (!StringUtils.hasText(escrow.getOutTradeNo())) {
            escrow.setOutTradeNo(buildPriceIncreaseOutTradeNo(change.getId()));
        }
        escrow.setPayBatchId(change.getId());
        escrow.setStatus(BountyEscrowStatus.WAIT_PAY.getCode());
        bountyEscrowRepository.updateById(escrow);
    }

    private String buildPriceIncreaseOutTradeNo(Long priceChangeId) {
        String suffix = String.valueOf(priceChangeId == null ? 0L : priceChangeId);
        if (suffix.length() > 8) {
            suffix = suffix.substring(suffix.length() - 8);
        }
        return "BPCH" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase()
                + suffix;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendMessage(BountyMessageSendRequest request, Long userId, String role) {
        BountyTask task = getTaskOrThrow(request.getTaskId());
        if (!canAccessTask(task.getId(), userId)) {
            throw new BusinessException("无权限发送消息");
        }

        BountyMessage message = new BountyMessage();
        message.setTaskId(task.getId());
        message.setSenderId(userId);
        message.setSenderRole(role);
        message.setMessageType(request.getMessageType() == null ? 1 : request.getMessageType());
        message.setContent(request.getContent());
        message.setAttachments(request.getAttachments());
        bountyMessageRepository.insert(message);
        bountyWebSocketService.broadcastMessage(task.getId(), toMessageVO(message));
        return message.getId();
    }

    @Override
    public PageResult<BountyMessageVO> pageMessages(Long taskId, Integer pageNum, Integer pageSize, Long userId) {
        BountyTask task = getTaskOrThrow(taskId);
        if (!canAccessTask(task.getId(), userId)) {
            throw new BusinessException("无权查看消息");
        }

        Page<BountyMessage> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize);
        Page<BountyMessage> result = bountyMessageRepository.selectPage(page, new LambdaQueryWrapper<BountyMessage>()
                .eq(BountyMessage::getTaskId, taskId)
                .eq(BountyMessage::getIsDelete, 0)
                .orderByDesc(BountyMessage::getCreateTime));

        List<BountyMessageVO> records = result.getRecords().stream().map(this::toMessageVO).collect(Collectors.toList());
        return PageResult.<BountyMessageVO>builder()
                .records(records)
                .total(result.getTotal())
                .pageNum((int) result.getCurrent())
                .pageSize((int) result.getSize())
                .pages((int) result.getPages())
                .build();
    }

    private BountyTask getTaskOrThrow(Long taskId) {
        BountyTask task = bountyTaskRepository.selectById(taskId);
        if (task == null || Objects.equals(task.getIsDelete(), 1)) {
            throw new BusinessException("悬赏任务不存在");
        }
        return task;
    }

    private boolean canAccessTask(Long taskId, Long userId) {
        BountyTask task = bountyTaskRepository.selectById(taskId);
        if (task == null || userId == null) {
            return false;
        }
        if (Objects.equals(task.getPublisherId(), userId) || Objects.equals(task.getWinnerDesignerId(), userId)) {
            return true;
        }
        Long bidCount = bountyBidRepository.selectCount(new LambdaQueryWrapper<BountyBid>()
                .eq(BountyBid::getTaskId, taskId)
                .eq(BountyBid::getDesignerId, userId)
                .eq(BountyBid::getIsDelete, 0));
        return bidCount != null && bidCount > 0;
    }

    private BountyTaskListVO toListVO(BountyTask task) {
        BountyTaskListVO vo = new BountyTaskListVO();
        BeanUtils.copyProperties(task, vo);
        Long count = bountyBidRepository.selectCount(new LambdaQueryWrapper<BountyBid>()
                .eq(BountyBid::getTaskId, task.getId())
                .eq(BountyBid::getIsDelete, 0));
        vo.setBidCount(count == null ? 0 : count.intValue());
        return vo;
    }

    private BountyBidVO toBidVO(BountyBid bid) {
        BountyBidVO vo = new BountyBidVO();
        BeanUtils.copyProperties(bid, vo);
        return vo;
    }

    private BountyMessageVO toMessageVO(BountyMessage message) {
        BountyMessageVO vo = new BountyMessageVO();
        BeanUtils.copyProperties(message, vo);
        return vo;
    }

    private LocalDateTime parseDateTime(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return LocalDateTime.parse(text.trim(), DATE_TIME_FORMATTER);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String buildTaskSn() {
        return "BT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }

    private void appendStatusLog(Long taskId, Integer fromStatus, Integer toStatus,
                                 Long operatorId, String operatorRole, String remark) {
        BountyStatusLog statusLog = new BountyStatusLog();
        statusLog.setTaskId(taskId);
        statusLog.setFromStatus(fromStatus);
        statusLog.setToStatus(toStatus);
        statusLog.setOperatorId(operatorId);
        statusLog.setOperatorRole(operatorRole);
        statusLog.setRemark(remark);
        bountyStatusLogRepository.insert(statusLog);
    }

    private void sendSystemMessage(Long taskId, String content) {
        BountyMessage message = new BountyMessage();
        message.setTaskId(taskId);
        message.setSenderId(0L);
        message.setSenderRole("SYSTEM");
        message.setMessageType(3);
        message.setContent(content);
        bountyMessageRepository.insert(message);
        bountyWebSocketService.broadcastMessage(taskId, toMessageVO(message));
    }
}
