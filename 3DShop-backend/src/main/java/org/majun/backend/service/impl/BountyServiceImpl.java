package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.dto.BountyAcceptRequest;
import org.majun.backend.dto.BountyBidCreateRequest;
import org.majun.backend.dto.BountyBidUpdateRequest;
import org.majun.backend.dto.BountyCancelRequest;
import org.majun.backend.dto.BountyCancelReviewRequest;
import org.majun.backend.dto.BountyDeliverySubmitRequest;
import org.majun.backend.dto.BountyMessageSendRequest;
import org.majun.backend.dto.BountyPickBidRequest;
import org.majun.backend.dto.BountyPriceChangeConfirmRequest;
import org.majun.backend.dto.BountyPriceChangeRequest;
import org.majun.backend.dto.BountyRatingAppealCreateRequest;
import org.majun.backend.dto.BountyRatingAppealReviewRequest;
import org.majun.backend.dto.BountyRatingCreateRequest;
import org.majun.backend.dto.BountyTaskCreateRequest;
import org.majun.backend.dto.BountyTaskQueryRequest;
import org.majun.backend.dto.BountyTaskResubmitRequest;
import org.majun.backend.dto.BountyTaskReviewRequest;
import org.majun.backend.entity.BountyBid;
import org.majun.backend.entity.BountyDelivery;
import org.majun.backend.entity.BountyDeliveryFile;
import org.majun.backend.entity.BountyEscrow;
import org.majun.backend.entity.BountyMessage;
import org.majun.backend.entity.BountyPriceChange;
import org.majun.backend.entity.BountyRating;
import org.majun.backend.entity.BountyRatingAppeal;
import org.majun.backend.entity.BountyStatusLog;
import org.majun.backend.entity.BountyTask;
import org.majun.backend.entity.BountyTaskAttachment;
import org.majun.backend.entity.DesignerReputation;
import org.majun.backend.enums.BountyBidStatus;
import org.majun.backend.enums.BountyDeliveryStatus;
import org.majun.backend.enums.BountyEscrowStatus;
import org.majun.backend.enums.BountyPriceChangeStatus;
import org.majun.backend.enums.BountyTaskStatus;
import org.majun.backend.repository.BountyBidRepository;
import org.majun.backend.repository.BountyDeliveryFileRepository;
import org.majun.backend.repository.BountyDeliveryRepository;
import org.majun.backend.repository.BountyEscrowRepository;
import org.majun.backend.repository.BountyMessageRepository;
import org.majun.backend.repository.BountyPriceChangeRepository;
import org.majun.backend.repository.BountyRatingAppealRepository;
import org.majun.backend.repository.BountyRatingRepository;
import org.majun.backend.repository.BountyStatusLogRepository;
import org.majun.backend.repository.BountyTaskRepository;
import org.majun.backend.repository.BountyTaskAttachmentRepository;
import org.majun.backend.repository.DesignerReputationRepository;
import org.majun.backend.service.BountyService;
import org.majun.backend.service.BountyWebSocketService;
import org.majun.backend.vo.BountyBidVO;
import org.majun.backend.vo.BountyMessageVO;
import org.majun.backend.vo.BountyRatingAppealVO;
import org.majun.backend.vo.BountyRatingVO;
import org.majun.backend.vo.BountyTaskDetailVO;
import org.majun.backend.vo.BountyTaskListVO;
import org.majun.backend.vo.DesignerReputationVO;
import org.majun.backend.vo.PageResult;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
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
    private final BountyRatingRepository bountyRatingRepository;
    private final DesignerReputationRepository designerReputationRepository;
    private final BountyRatingAppealRepository bountyRatingAppealRepository;
    private final BountyDeliveryFileRepository bountyDeliveryFileRepository;

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
        task.setStatus(BountyTaskStatus.PENDING_REVIEW.getCode());
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
        escrow.setStatus(BountyEscrowStatus.WAIT_PAY.getCode());
        escrow.setVersion(0);
        bountyEscrowRepository.insert(escrow);

        appendStatusLog(task.getId(), null, task.getStatus(), userId, "USER", "创建悬赏任务，等待平台审核");
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
        // 待审核和待支付托管的任务只有发布者自己能看到，其他人只能看到状态>=1的任务
        wrapper.and(w -> w
                .ge(BountyTask::getStatus, BountyTaskStatus.RECRUITING.getCode())
                .or(o -> o.eq(BountyTask::getPublisherId, userId)
                        .in(BountyTask::getStatus,
                                BountyTaskStatus.PENDING_REVIEW.getCode(),
                                BountyTaskStatus.WAIT_ESCROW_PAYMENT.getCode()))
        );

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
    public PageResult<BountyTaskListVO> pageTasksForAdmin(BountyTaskQueryRequest request) {
        Page<BountyTask> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<BountyTask> wrapper = new LambdaQueryWrapper<BountyTask>()
                .eq(BountyTask::getIsDelete, 0)
                .orderByDesc(BountyTask::getCreateTime);

        // 管理员可以按状态筛选
        if (request.getStatus() != null) {
            wrapper.eq(BountyTask::getStatus, request.getStatus());
        }
        // 管理员可以按关键词搜索
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(BountyTask::getTitle, request.getKeyword().trim());
        }
        // 管理员可以看到所有状态的任务，不隐藏任何状态

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
        // 待审核和待支付托管状态的任务只有发布者能查看
        if (Objects.equals(task.getStatus(), BountyTaskStatus.PENDING_REVIEW.getCode())
                || Objects.equals(task.getStatus(), BountyTaskStatus.WAIT_ESCROW_PAYMENT.getCode())) {
            if (!Objects.equals(task.getPublisherId(), userId)) {
                throw new BusinessException("该任务暂不可查看");
            }
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
            if (!Objects.equals(task.getStatus(), BountyTaskStatus.PENDING_REVIEW.getCode())) {
                throw new BusinessException("当前任务状态不是待审核");
            }
            task.setStatus(BountyTaskStatus.WAIT_ESCROW_PAYMENT.getCode());
            task.setCloseReason(null);
            bountyTaskRepository.updateById(task);
            appendStatusLog(task.getId(), beforeStatus, task.getStatus(), adminId, "ADMIN",
                    StringUtils.hasText(request.getRemark()) ? request.getRemark() : "管理员审核通过，等待支付托管金");
            sendSystemMessage(task.getId(), "任务已通过平台审核，请支付托管金");
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resubmitTask(BountyTaskResubmitRequest request, Long userId) {
        BountyTask task = getTaskOrThrow(request.getTaskId());
        if (!Objects.equals(task.getPublisherId(), userId)) {
            throw new BusinessException("仅发布者可重新提交");
        }
        if (!Objects.equals(task.getStatus(), BountyTaskStatus.CLOSED.getCode())) {
            throw new BusinessException("仅被驳回的任务可重新提交");
        }

        Integer beforeStatus = task.getStatus();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setCategory(request.getCategory());
        task.setTags(request.getTags());
        task.setBudgetAmount(request.getBudgetAmount());
        task.setFinalAmount(request.getBudgetAmount());
        if (request.getExpectedDays() != null) {
            task.setExpectedDays(request.getExpectedDays());
        }
        if (StringUtils.hasText(request.getDeadlineTime())) {
            task.setDeadlineTime(parseDateTime(request.getDeadlineTime()));
        }
        task.setStatus(BountyTaskStatus.PENDING_REVIEW.getCode());
        task.setCloseReason(null);
        bountyTaskRepository.updateById(task);

        // 更新附件：先删旧的再插新的
        if (request.getAttachments() != null) {
            bountyTaskAttachmentRepository.update(null, new LambdaUpdateWrapper<BountyTaskAttachment>()
                    .eq(BountyTaskAttachment::getTaskId, task.getId())
                    .set(BountyTaskAttachment::getIsDelete, 1));
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

        // 更新托管金金额（预算可能变了）
        BountyEscrow escrow = bountyEscrowRepository.selectOne(new LambdaQueryWrapper<BountyEscrow>()
                .eq(BountyEscrow::getTaskId, task.getId())
                .eq(BountyEscrow::getIsDelete, 0)
                .last("limit 1"));
        if (escrow != null) {
            escrow.setEscrowAmount(request.getBudgetAmount());
            bountyEscrowRepository.updateById(escrow);
        }

        appendStatusLog(task.getId(), beforeStatus, task.getStatus(), userId, "USER", "修改后重新提交审核");
        sendSystemMessage(task.getId(), "任务已修改并重新提交审核");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void requestCancelTask(BountyCancelRequest request, Long userId) {
        BountyTask task = getTaskOrThrow(request.getTaskId());
        if (!Objects.equals(task.getPublisherId(), userId)) {
            throw new BusinessException("仅发布者可申请取消");
        }
        if (!Objects.equals(task.getStatus(), BountyTaskStatus.RECRUITING.getCode())
                && !Objects.equals(task.getStatus(), BountyTaskStatus.PICKED.getCode())) {
            throw new BusinessException("当前状态不可取消，仅招募中或已选标状态可申请取消");
        }
        if (Objects.equals(task.getCancelRequested(), 1)) {
            throw new BusinessException("已提交取消申请，请等待审核");
        }

        task.setCancelRequested(1);
        if (request.getReason() != null && !request.getReason().isBlank()) {
            task.setCloseReason(request.getReason());
        }
        bountyTaskRepository.updateById(task);
        appendStatusLog(task.getId(), task.getStatus(), task.getStatus(), userId, "USER", "申请取消悬赏");
        sendSystemMessage(task.getId(), "发布者已申请取消悬赏，等待管理员审核");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewCancelTask(BountyCancelReviewRequest request, Long adminId) {
        BountyTask task = getTaskOrThrow(request.getTaskId());
        if (!Objects.equals(task.getCancelRequested(), 1)) {
            throw new BusinessException("该任务没有待处理的取消申请");
        }

        Integer beforeStatus = task.getStatus();
        if (Objects.equals(request.getDecision(), 1)) {
            task.setStatus(BountyTaskStatus.CLOSED.getCode());
            task.setCancelRequested(0);
            if (request.getRemark() != null && !request.getRemark().isBlank()) {
                task.setCloseReason(request.getRemark());
            }
            bountyTaskRepository.updateById(task);

            bountyFinanceService.refundToPublisher(task);

            appendStatusLog(task.getId(), beforeStatus, task.getStatus(), adminId, "ADMIN", "同意取消，托管金已退回");
            sendSystemMessage(task.getId(), "取消申请已通过，托管金已退回您的钱包");
        } else if (Objects.equals(request.getDecision(), 2)) {
            task.setCancelRequested(0);
            task.setCloseReason(null);
            bountyTaskRepository.updateById(task);
            appendStatusLog(task.getId(), beforeStatus, task.getStatus(), adminId, "ADMIN", "拒绝取消申请");
            sendSystemMessage(task.getId(), "取消申请已被拒绝，任务继续进行");
        } else {
            throw new BusinessException("决策仅支持1或2");
        }
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

        // 查询最新交付记录（不限状态）填充交付信息
        BountyDelivery latestDelivery = bountyDeliveryRepository.selectOne(new LambdaQueryWrapper<BountyDelivery>()
            .eq(BountyDelivery::getTaskId, taskId)
            .eq(BountyDelivery::getIsDelete, 0)
            .orderByDesc(BountyDelivery::getDeliveryRound)
            .orderByDesc(BountyDelivery::getCreateTime)
            .last("limit 1"));
        if (latestDelivery != null) {
            BountyTaskDetailVO.DeliveryInfo info = new BountyTaskDetailVO.DeliveryInfo();
            info.setId(latestDelivery.getId());
            info.setDeliveryRound(latestDelivery.getDeliveryRound());
            info.setDescription(latestDelivery.getDescription());
            info.setStatus(latestDelivery.getStatus());
            info.setIsFinal(latestDelivery.getIsFinal());
            info.setAllowCommercialUse(latestDelivery.getAllowCommercialUse());
            info.setAllowModification(latestDelivery.getAllowModification());
            info.setLicenseType(latestDelivery.getLicenseType());
            info.setCreateTime(latestDelivery.getCreateTime());

            // 仅任务已完成时暴露文件URL
            List<BountyDeliveryFile> files = bountyDeliveryFileRepository.selectList(new LambdaQueryWrapper<BountyDeliveryFile>()
                .eq(BountyDeliveryFile::getDeliveryId, latestDelivery.getId())
                .eq(BountyDeliveryFile::getIsDelete, 0)
                .orderByAsc(BountyDeliveryFile::getSortNo));
            if (Objects.equals(task.getStatus(), BountyTaskStatus.COMPLETED.getCode())) {
                info.setFiles(files.stream().map(f -> {
                    BountyTaskDetailVO.DeliveryFileItem item = new BountyTaskDetailVO.DeliveryFileItem();
                    item.setUrl(f.getFileUrl());
                    item.setName(f.getFileName());
                    item.setType(f.getFileType());
                    return item;
                }).collect(Collectors.toList()));
            } else {
                info.setFiles(Collections.emptyList());
            }
            vo.setDeliveryInfo(info);
        }

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
        delivery.setAllowCommercialUse(request.getAllowCommercialUse() == null ? 0 : request.getAllowCommercialUse());
        delivery.setAllowModification(request.getAllowModification() == null ? 1 : request.getAllowModification());
        delivery.setLicenseType(request.getLicenseType() == null ? "Personal" : request.getLicenseType());
        bountyDeliveryRepository.insert(delivery);

        if (request.getFileUrls() != null && !request.getFileUrls().isEmpty()) {
            int sortNo = 0;
            for (String fileUrl : request.getFileUrls()) {
                if (!StringUtils.hasText(fileUrl)) {
                    continue;
                }
                BountyDeliveryFile file = new BountyDeliveryFile();
                file.setDeliveryId(delivery.getId());
                file.setFileUrl(fileUrl.trim());
                String cleanUrl = fileUrl.trim().split("\\?")[0].split("#")[0];
                String fileName = cleanUrl.contains("/") ? cleanUrl.substring(cleanUrl.lastIndexOf('/') + 1) : cleanUrl;
                file.setFileName(fileName);
                String fileType = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase() : "";
                file.setFileType(fileType);
                file.setSortNo(sortNo++);
                bountyDeliveryFileRepository.insert(file);
            }
        }

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

    // ==================== 评价相关实现 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRating(BountyRatingCreateRequest request, Long userId) {
        BountyTask task = getTaskOrThrow(request.getTaskId());
        if (!Objects.equals(task.getStatus(), BountyTaskStatus.COMPLETED.getCode())) {
            throw new BusinessException("仅已完成的任务可以评价");
        }
        if (!Objects.equals(task.getPublisherId(), userId)) {
            throw new BusinessException("仅发布者可以评价");
        }

        BountyRating existed = bountyRatingRepository.selectOne(new LambdaQueryWrapper<BountyRating>()
                .eq(BountyRating::getTaskId, request.getTaskId())
                .eq(BountyRating::getIsDelete, 0));
        if (existed != null) {
            throw new BusinessException("该任务已评价");
        }

        if (task.getWinnerDesignerId() == null) {
            throw new BusinessException("任务未选定设计者，无法评价");
        }

        BountyRating rating = new BountyRating();
        rating.setTaskId(request.getTaskId());
        rating.setPublisherId(userId);
        rating.setDesignerId(task.getWinnerDesignerId());
        rating.setScore(request.getScore());
        rating.setComment(request.getComment());
        rating.setImages(request.getImages());
        rating.setIsAnonymous(request.getIsAnonymous() == null ? 0 : request.getIsAnonymous());
        rating.setStatus(1);
        bountyRatingRepository.insert(rating);

        updateDesignerReputation(task.getWinnerDesignerId(), request.getScore(), true);

        sendSystemMessage(request.getTaskId(), "发布者已完成评价");
        return rating.getId();
    }

    @Override
    public BountyRatingVO getRatingByTask(Long taskId, Long userId) {
        BountyTask task = getTaskOrThrow(taskId);
        BountyRating rating = bountyRatingRepository.selectOne(new LambdaQueryWrapper<BountyRating>()
                .eq(BountyRating::getTaskId, taskId)
                .eq(BountyRating::getIsDelete, 0));
        if (rating == null) {
            return null;
        }
        return toRatingVO(rating, task);
    }

    @Override
    public PageResult<BountyRatingVO> getDesignerRatings(Long designerId, Integer pageNum, Integer pageSize) {
        Page<BountyRating> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        Page<BountyRating> result = bountyRatingRepository.selectPage(page, new LambdaQueryWrapper<BountyRating>()
                .eq(BountyRating::getDesignerId, designerId)
                .eq(BountyRating::getStatus, 1)
                .eq(BountyRating::getIsDelete, 0)
                .orderByDesc(BountyRating::getCreateTime));

        List<BountyRatingVO> records = result.getRecords().stream()
                .map(rating -> {
                    BountyTask task = bountyTaskRepository.selectById(rating.getTaskId());
                    return toRatingVO(rating, task);
                })
                .collect(Collectors.toList());

        return PageResult.<BountyRatingVO>builder()
                .records(records)
                .total(result.getTotal())
                .pageNum((int) result.getCurrent())
                .pageSize((int) result.getSize())
                .pages((int) result.getPages())
                .build();
    }

    // ==================== 申诉相关实现 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRatingAppeal(BountyRatingAppealCreateRequest request, Long userId) {
        BountyRating rating = bountyRatingRepository.selectById(request.getRatingId());
        if (rating == null || Objects.equals(rating.getIsDelete(), 1)) {
            throw new BusinessException("评价记录不存在");
        }
        if (!Objects.equals(rating.getDesignerId(), userId)) {
            throw new BusinessException("仅被评价设计者可以申诉");
        }
        if (!Objects.equals(rating.getStatus(), 1)) {
            throw new BusinessException("该评价已无效，无需申诉");
        }

        BountyRatingAppeal existed = bountyRatingAppealRepository.selectOne(new LambdaQueryWrapper<BountyRatingAppeal>()
                .eq(BountyRatingAppeal::getRatingId, request.getRatingId())
                .eq(BountyRatingAppeal::getStatus, 0)
                .eq(BountyRatingAppeal::getIsDelete, 0));
        if (existed != null) {
            throw new BusinessException("该评价已有待处理的申诉");
        }

        BountyRatingAppeal appeal = new BountyRatingAppeal();
        appeal.setRatingId(request.getRatingId());
        appeal.setDesignerId(userId);
        appeal.setReason(request.getReason());
        appeal.setEvidence(request.getEvidence());
        appeal.setStatus(0);
        bountyRatingAppealRepository.insert(appeal);
        return appeal.getId();
    }

    @Override
    public PageResult<BountyRatingAppealVO> getMyAppeals(Long userId, Integer pageNum, Integer pageSize) {
        Page<BountyRatingAppeal> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        Page<BountyRatingAppeal> result = bountyRatingAppealRepository.selectPage(page, new LambdaQueryWrapper<BountyRatingAppeal>()
                .eq(BountyRatingAppeal::getDesignerId, userId)
                .eq(BountyRatingAppeal::getIsDelete, 0)
                .orderByDesc(BountyRatingAppeal::getCreateTime));

        List<BountyRatingAppealVO> records = result.getRecords().stream()
                .map(this::toAppealVO)
                .collect(Collectors.toList());

        return PageResult.<BountyRatingAppealVO>builder()
                .records(records)
                .total(result.getTotal())
                .pageNum((int) result.getCurrent())
                .pageSize((int) result.getSize())
                .pages((int) result.getPages())
                .build();
    }

    @Override
    public PageResult<BountyRatingAppealVO> getAllAppeals(Integer pageNum, Integer pageSize, Integer status) {
        Page<BountyRatingAppeal> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        LambdaQueryWrapper<BountyRatingAppeal> wrapper = new LambdaQueryWrapper<BountyRatingAppeal>()
                .eq(BountyRatingAppeal::getIsDelete, 0);
        if (status != null) {
            wrapper.eq(BountyRatingAppeal::getStatus, status);
        }
        wrapper.orderByDesc(BountyRatingAppeal::getCreateTime);

        Page<BountyRatingAppeal> result = bountyRatingAppealRepository.selectPage(page, wrapper);
        List<BountyRatingAppealVO> records = result.getRecords().stream()
                .map(this::toAppealVO)
                .collect(Collectors.toList());

        return PageResult.<BountyRatingAppealVO>builder()
                .records(records)
                .total(result.getTotal())
                .pageNum((int) result.getCurrent())
                .pageSize((int) result.getSize())
                .pages((int) result.getPages())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewRatingAppeal(BountyRatingAppealReviewRequest request, Long adminId) {
        BountyRatingAppeal appeal = bountyRatingAppealRepository.selectById(request.getAppealId());
        if (appeal == null || Objects.equals(appeal.getIsDelete(), 1)) {
            throw new BusinessException("申诉记录不存在");
        }
        if (!Objects.equals(appeal.getStatus(), 0)) {
            throw new BusinessException("该申诉已处理");
        }

        BountyRating rating = bountyRatingRepository.selectById(appeal.getRatingId());
        if (rating == null) {
            throw new BusinessException("原评价记录不存在");
        }

        appeal.setAdminId(adminId);
        appeal.setAdminRemark(request.getAdminRemark());
        appeal.setProcessedTime(LocalDateTime.now());

        if (Objects.equals(request.getDecision(), 1)) {
            appeal.setStatus(1);
            rating.setStatus(0);
            rating.setAdminRemark(request.getAdminRemark());
            bountyRatingRepository.updateById(rating);

            updateDesignerReputationForAppeal(rating);
        } else if (Objects.equals(request.getDecision(), 2)) {
            appeal.setStatus(2);
        } else {
            throw new BusinessException("审核结论仅支持1或2");
        }

        bountyRatingAppealRepository.updateById(appeal);
    }

    // ==================== 信誉相关实现 ====================

    @Override
    public DesignerReputationVO getDesignerReputation(Long designerId) {
        DesignerReputation reputation = designerReputationRepository.selectOne(new LambdaQueryWrapper<DesignerReputation>()
                .eq(DesignerReputation::getDesignerId, designerId)
                .eq(DesignerReputation::getIsDelete, 0));
        if (reputation == null) {
            DesignerReputationVO vo = new DesignerReputationVO();
            vo.setDesignerId(designerId);
            vo.setReputationScore(80);
            vo.setTotalTasks(0);
            vo.setTotalRatings(0);
            vo.setFiveStarCount(0);
            vo.setFourStarCount(0);
            vo.setThreeStarCount(0);
            vo.setTwoStarCount(0);
            vo.setOneStarCount(0);
            vo.setQualityAnswerCount(0);
            return vo;
        }
        return toReputationVO(reputation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReputationForQualityAnswer(Long designerId) {
        DesignerReputation reputation = getOrCreateReputation(designerId);
        reputation.setQualityAnswerCount(reputation.getQualityAnswerCount() + 1);
        reputation.setReputationScore(Math.min(100, reputation.getReputationScore() + 3));
        designerReputationRepository.updateById(reputation);
    }

    // ==================== 私有辅助方法 ====================

    private DesignerReputation getOrCreateReputation(Long designerId) {
        DesignerReputation reputation = designerReputationRepository.selectOne(new LambdaQueryWrapper<DesignerReputation>()
                .eq(DesignerReputation::getDesignerId, designerId)
                .eq(DesignerReputation::getIsDelete, 0));
        if (reputation == null) {
            reputation = new DesignerReputation();
            reputation.setDesignerId(designerId);
            reputation.setReputationScore(80);
            reputation.setTotalTasks(0);
            reputation.setTotalRatings(0);
            reputation.setFiveStarCount(0);
            reputation.setFourStarCount(0);
            reputation.setThreeStarCount(0);
            reputation.setTwoStarCount(0);
            reputation.setOneStarCount(0);
            reputation.setQualityAnswerCount(0);
            designerReputationRepository.insert(reputation);
        }
        return reputation;
    }

    private void updateDesignerReputation(Long designerId, Integer score, boolean isAdd) {
        DesignerReputation reputation = getOrCreateReputation(designerId);

        if (isAdd) {
            reputation.setTotalRatings(reputation.getTotalRatings() + 1);
            switch (score) {
                case 5: reputation.setFiveStarCount(reputation.getFiveStarCount() + 1); break;
                case 4: reputation.setFourStarCount(reputation.getFourStarCount() + 1); break;
                case 3: reputation.setThreeStarCount(reputation.getThreeStarCount() + 1); break;
                case 2: reputation.setTwoStarCount(reputation.getTwoStarCount() + 1); break;
                case 1: reputation.setOneStarCount(reputation.getOneStarCount() + 1); break;
            }

            if (score >= 4) {
                reputation.setReputationScore(Math.min(100, reputation.getReputationScore() + 1));
            }
        }

        reputation.setTotalTasks(reputation.getTotalTasks() + (isAdd ? 1 : 0));

        int total = reputation.getFiveStarCount() + reputation.getFourStarCount() +
                    reputation.getThreeStarCount() + reputation.getTwoStarCount() + reputation.getOneStarCount();
        if (total > 0) {
            BigDecimal avg = BigDecimal.valueOf(
                (5.0 * reputation.getFiveStarCount() + 4.0 * reputation.getFourStarCount() +
                 3.0 * reputation.getThreeStarCount() + 2.0 * reputation.getTwoStarCount() +
                 1.0 * reputation.getOneStarCount()) / total
            ).setScale(2, java.math.RoundingMode.HALF_UP);
            reputation.setAvgScore(avg);
        }

        designerReputationRepository.updateById(reputation);
    }

    private void updateDesignerReputationForAppeal(BountyRating rating) {
        DesignerReputation reputation = getOrCreateReputation(rating.getDesignerId());

        reputation.setTotalRatings(Math.max(0, reputation.getTotalRatings() - 1));
        switch (rating.getScore()) {
            case 5: reputation.setFiveStarCount(Math.max(0, reputation.getFiveStarCount() - 1)); break;
            case 4: reputation.setFourStarCount(Math.max(0, reputation.getFourStarCount() - 1)); break;
            case 3: reputation.setThreeStarCount(Math.max(0, reputation.getThreeStarCount() - 1)); break;
            case 2: reputation.setTwoStarCount(Math.max(0, reputation.getTwoStarCount() - 1)); break;
            case 1: reputation.setOneStarCount(Math.max(0, reputation.getOneStarCount() - 1)); break;
        }

        if (rating.getScore() >= 4) {
            reputation.setReputationScore(Math.max(0, reputation.getReputationScore() - 1));
        }

        int total = reputation.getFiveStarCount() + reputation.getFourStarCount() +
                    reputation.getThreeStarCount() + reputation.getTwoStarCount() + reputation.getOneStarCount();
        if (total > 0) {
            BigDecimal avg = BigDecimal.valueOf(
                (5.0 * reputation.getFiveStarCount() + 4.0 * reputation.getFourStarCount() +
                 3.0 * reputation.getThreeStarCount() + 2.0 * reputation.getTwoStarCount() +
                 1.0 * reputation.getOneStarCount()) / total
            ).setScale(2, java.math.RoundingMode.HALF_UP);
            reputation.setAvgScore(avg);
        } else {
            reputation.setAvgScore(null);
        }

        designerReputationRepository.updateById(reputation);
    }

    private BountyRatingVO toRatingVO(BountyRating rating, BountyTask task) {
        BountyRatingVO vo = new BountyRatingVO();
        BeanUtils.copyProperties(rating, vo);
        if (rating.getImages() != null) {
            vo.setImages(List.of(rating.getImages().split(",")));
        }
        if (task != null) {
            vo.setTaskTitle(task.getTitle());
        }
        if (Objects.equals(rating.getIsAnonymous(), 1)) {
            vo.setPublisherName("匿名用户");
        }
        return vo;
    }

    private BountyRatingAppealVO toAppealVO(BountyRatingAppeal appeal) {
        BountyRatingAppealVO vo = new BountyRatingAppealVO();
        BeanUtils.copyProperties(appeal, vo);

        BountyRating rating = bountyRatingRepository.selectById(appeal.getRatingId());
        if (rating != null) {
            vo.setRatingScore(rating.getScore());
            vo.setRatingComment(rating.getComment());
            vo.setTaskId(rating.getTaskId());
            BountyTask task = bountyTaskRepository.selectById(rating.getTaskId());
            if (task != null) {
                vo.setTaskTitle(task.getTitle());
            }
        }

        if (appeal.getEvidence() != null) {
            vo.setEvidence(List.of(appeal.getEvidence().split(",")));
        }
        return vo;
    }

    private DesignerReputationVO toReputationVO(DesignerReputation reputation) {
        DesignerReputationVO vo = new DesignerReputationVO();
        BeanUtils.copyProperties(reputation, vo);
        return vo;
    }
}
