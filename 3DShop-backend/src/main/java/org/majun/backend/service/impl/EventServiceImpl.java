package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.ResultCode;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.dto.*;
import org.majun.backend.entity.*;
import org.majun.backend.repository.*;
import org.majun.backend.service.EventService;
import org.majun.backend.service.PointService;
import org.majun.backend.vo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 活动赛事服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
/**
 * 活动赛事服务实现
 */
public class EventServiceImpl implements EventService {

    private final SysEventRepository eventRepository;
    private final SysEventRewardRepository rewardRepository;
    private final SysEventParticipationRepository participationRepository;
    private final SysEventSubmissionRepository submissionRepository;
    private final SysEventSubmissionLikeRepository submissionLikeRepository;
    private final SysEventSubmissionCommentRepository submissionCommentRepository;
    private final SysEventCommentLikeRepository commentLikeRepository;
    private final SysUserRepository userRepository;
    private final PointService pointService;

    // ==================== 管理端接口 ====================

    @Override
    public PageResult<EventVO> getAdminEventList(EventQueryRequest request) {
        Page<SysEvent> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<SysEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(request.getKeyword()), SysEvent::getTitle, request.getKeyword());
        wrapper.eq(request.getEventType() != null, SysEvent::getEventType, request.getEventType());

        // 管理端支持数字状态筛选
        Integer statusValue = parseStatusValue(request.getStatus());
        wrapper.eq(statusValue != null, SysEvent::getStatus, statusValue);

        wrapper.orderByDesc(SysEvent::getCreateTime);

        Page<SysEvent> result = eventRepository.selectPage(page, wrapper);

        List<EventVO> records = result.getRecords().stream()
                .map(this::convertToEventVO)
                .collect(Collectors.toList());

        return PageResult.<EventVO>builder()
                .records(records)
                .total(result.getTotal())
                .pageNum((int) result.getCurrent())
                .pageSize((int) result.getSize())
                .pages((int) result.getPages())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEvent(EventCreateRequest request, Long createBy) {
        // 校验时间
        validateEventTime(request.getStartTime(), request.getEndTime(),
                request.getSignupStart(), request.getSignupEnd());

        SysEvent event = new SysEvent();
        event.setTitle(request.getTitle());
        event.setBannerUrl(request.getBannerUrl());
        event.setEventType(request.getEventType());
        event.setDescription(request.getDescription());
        event.setRules(request.getRules());
        event.setLocation(request.getLocation());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setSignupStart(request.getSignupStart());
        event.setSignupEnd(request.getSignupEnd());
        event.setMaxParticipants(request.getMaxParticipants());
        event.setCurrentParticipants(0);
        event.setStatus(calculateInitStatus(request));
        event.setCreateBy(createBy);
        event.setIsDelete(0);

        eventRepository.insert(event);

        // 保存奖励
        saveRewards(event.getId(), request.getRewards());

        return event.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEvent(EventUpdateRequest request) {
        SysEvent event = getEventOrThrow(request.getId());

        // 校验时间
        validateEventTime(request.getStartTime(), request.getEndTime(),
                request.getSignupStart(), request.getSignupEnd());

        event.setTitle(request.getTitle());
        event.setBannerUrl(request.getBannerUrl());
        event.setEventType(request.getEventType());
        event.setDescription(request.getDescription());
        event.setRules(request.getRules());
        event.setLocation(request.getLocation());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setSignupStart(request.getSignupStart());
        event.setSignupEnd(request.getSignupEnd());
        event.setMaxParticipants(request.getMaxParticipants());
        if (request.getStatus() != null) {
            event.setStatus(request.getStatus());
        }

        eventRepository.updateById(event);

        // 删除旧奖励，保存新奖励
        LambdaQueryWrapper<SysEventReward> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysEventReward::getEventId, event.getId());
        rewardRepository.delete(wrapper);

        saveRewards(event.getId(), request.getRewards());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEventStatus(EventStatusUpdateRequest request) {
        SysEvent event = getEventOrThrow(request.getId());
        event.setStatus(request.getStatus());
        eventRepository.updateById(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEvent(Long id) {
        SysEvent event = getEventOrThrow(id);
        event.setIsDelete(1);
        eventRepository.updateById(event);
    }

    // ==================== 移动端接口 ====================

    @Override
    public PageResult<EventListVO> getEventList(EventQueryRequest request) {
        // 兼容 page/size 参数
        int pageNum = request.getPage() != null ? request.getPage() : request.getPageNum();
        int pageSize = request.getSize() != null ? request.getSize() : request.getPageSize();

        Page<SysEvent> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<SysEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(request.getKeyword()), SysEvent::getTitle, request.getKeyword());
        wrapper.eq(request.getEventType() != null, SysEvent::getEventType, request.getEventType());

        // 处理字符串状态参数
        Integer statusFilter = convertStatusParam(request.getStatus());
        if (statusFilter != null) {
            if (statusFilter == -1) {
                // ongoing: 报名中、进行中、评审中
                wrapper.in(SysEvent::getStatus, 1, 2, 3);
            } else {
                wrapper.eq(SysEvent::getStatus, statusFilter);
            }
        }

        wrapper.orderByDesc(SysEvent::getCreateTime);

        Page<SysEvent> result = eventRepository.selectPage(page, wrapper);

        List<EventListVO> records = result.getRecords().stream()
                .map(this::convertToEventListVO)
                .collect(Collectors.toList());

        return PageResult.<EventListVO>builder()
                .records(records)
                .total(result.getTotal())
                .pageNum((int) result.getCurrent())
                .pageSize((int) result.getSize())
                .pages((int) result.getPages())
                .build();
    }

    @Override
    public EventDetailVO getEventDetail(Long id, Long userId) {
        SysEvent event = getEventOrThrow(id);

        EventDetailVO vo = new EventDetailVO();
        vo.setId(event.getId());
        vo.setTitle(event.getTitle());
        vo.setBannerUrl(event.getBannerUrl());
        vo.setEventType(event.getEventType());
        vo.setEventTypeName(getEventTypeName(event.getEventType()));
        vo.setDescription(event.getDescription());
        vo.setRules(event.getRules());
        vo.setLocation(event.getLocation());
        vo.setStartTime(event.getStartTime());
        vo.setEndTime(event.getEndTime());
        vo.setSignupStart(event.getSignupStart());
        vo.setSignupEnd(event.getSignupEnd());
        vo.setMaxParticipants(event.getMaxParticipants());
        vo.setCurrentParticipants(event.getCurrentParticipants());
        vo.setStatus(event.getStatus());
        vo.setStatusName(getStatusName(event.getStatus()));
        vo.setCreateTime(event.getCreateTime());

        // 查询奖励
        List<SysEventReward> rewards = rewardRepository.selectList(
                new LambdaQueryWrapper<SysEventReward>()
                        .eq(SysEventReward::getEventId, id)
                        .orderByAsc(SysEventReward::getRankOrder)
        );
        vo.setRewards(rewards.stream().map(r -> {
            EventRewardVO rvo = new EventRewardVO();
            rvo.setRankName(r.getRankName());
            rvo.setWinnerCount(r.getWinnerCount());
            rvo.setPrizeContent(r.getPrizeContent());
            rvo.setPoints(r.getPoints());
            return rvo;
        }).collect(Collectors.toList()));

        // 查询当前用户是否已报名
        if (userId != null) {
            LambdaQueryWrapper<SysEventParticipation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysEventParticipation::getEventId, id)
                    .eq(SysEventParticipation::getUserId, userId)
                    .ne(SysEventParticipation::getStatus, 5); // 排除已取消
            vo.setSignedUp(participationRepository.selectCount(wrapper) > 0);
        } else {
            vo.setSignedUp(false);
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void signupEvent(Long eventId, Long userId) {
        SysEvent event = getEventOrThrow(eventId);

        // 校验活动状态
        if (event.getStatus() != 1) {
            throw new BusinessException(ResultCode.FAIL, "当前活动不在报名阶段");
        }

        // 查询是否已有报名记录（包括已取消的）
        LambdaQueryWrapper<SysEventParticipation> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(SysEventParticipation::getEventId, eventId)
                .eq(SysEventParticipation::getUserId, userId);
        SysEventParticipation existParticipation = participationRepository.selectOne(existWrapper);

        if (existParticipation != null && existParticipation.getStatus() != 5) {
            // 已报名且未取消
            throw new BusinessException(ResultCode.FAIL, "您已报名该活动");
        }

        // 校验人数限制
        if (event.getMaxParticipants() != null &&
                event.getCurrentParticipants() >= event.getMaxParticipants()) {
            throw new BusinessException(ResultCode.FAIL, "活动报名人数已满");
        }

        if (existParticipation != null) {
            // 存在已取消的记录，复用该记录
            existParticipation.setSignupTime(LocalDateTime.now());
            existParticipation.setStatus(1);
            existParticipation.setAwardRank(null);
            existParticipation.setResult(null);
            participationRepository.updateById(existParticipation);
        } else {
            // 创建新的报名记录
            SysEventParticipation participation = new SysEventParticipation();
            participation.setEventId(eventId);
            participation.setUserId(userId);
            participation.setSignupTime(LocalDateTime.now());
            participation.setStatus(1);
            participation.setIsDelete(0);
            participationRepository.insert(participation);
        }

        // 更新参与人数
        event.setCurrentParticipants(event.getCurrentParticipants() + 1);
        eventRepository.updateById(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelSignup(Long eventId, Long userId) {
        SysEvent event = getEventOrThrow(eventId);

        // 查询报名记录
        LambdaQueryWrapper<SysEventParticipation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysEventParticipation::getEventId, eventId)
                .eq(SysEventParticipation::getUserId, userId)
                .ne(SysEventParticipation::getStatus, 5);
        SysEventParticipation participation = participationRepository.selectOne(wrapper);

        if (participation == null) {
            throw new BusinessException(ResultCode.FAIL, "您未报名该活动");
        }

        // 更新状态为已取消
        participation.setStatus(5);
        participationRepository.updateById(participation);

        // 更新参与人数
        event.setCurrentParticipants(Math.max(0, event.getCurrentParticipants() - 1));
        eventRepository.updateById(event);
    }

    @Override
    public PageResult<EventParticipationVO> getMyParticipations(Long userId, EventQueryRequest request) {
        Page<SysEventParticipation> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<SysEventParticipation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysEventParticipation::getUserId, userId)
                .ne(SysEventParticipation::getStatus, 5)
                .orderByDesc(SysEventParticipation::getSignupTime);

        Page<SysEventParticipation> result = participationRepository.selectPage(page, wrapper);

        List<EventParticipationVO> records = result.getRecords().stream()
                .map(p -> convertToParticipationVO(p, userId))
                .collect(Collectors.toList());

        return PageResult.<EventParticipationVO>builder()
                .records(records)
                .total(result.getTotal())
                .pageNum((int) result.getCurrent())
                .pageSize((int) result.getSize())
                .pages((int) result.getPages())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSubmission(EventSubmissionCreateRequest request, Long userId) {
        SysEvent event = getEventOrThrow(request.getEventId());

        // 校验活动类型（只有设计竞赛可以提交作品）
        if (event.getEventType() != 1) {
            throw new BusinessException(ResultCode.FAIL, "该活动不支持提交作品");
        }

        // 校验是否已报名
        LambdaQueryWrapper<SysEventParticipation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysEventParticipation::getEventId, request.getEventId())
                .eq(SysEventParticipation::getUserId, userId)
                .ne(SysEventParticipation::getStatus, 5);
        SysEventParticipation participation = participationRepository.selectOne(wrapper);

        if (participation == null) {
            throw new BusinessException(ResultCode.FAIL, "请先报名参加活动");
        }

        // 检查是否已提交过作品
        LambdaQueryWrapper<SysEventSubmission> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(SysEventSubmission::getEventId, request.getEventId())
                .eq(SysEventSubmission::getUserId, userId)
                .eq(SysEventSubmission::getIsDelete, 0);
        if (submissionRepository.selectCount(existWrapper) > 0) {
            throw new BusinessException(ResultCode.FAIL, "您已提交过作品，请勿重复提交");
        }

        // 创建作品提交
        SysEventSubmission submission = new SysEventSubmission();
        submission.setEventId(request.getEventId());
        submission.setParticipationId(participation.getId());
        submission.setUserId(userId);
        submission.setTitle(request.getTitle());
        submission.setDescription(request.getDescription());
        if (request.getFileUrls() != null && !request.getFileUrls().isEmpty()) {
            submission.setFileUrls(String.join(",", request.getFileUrls()));
        }
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            submission.setImageUrls(String.join(",", request.getImageUrls()));
        }
        submission.setStatus(1);
        submission.setIsDelete(0);
        submissionRepository.insert(submission);

        // 更新参与状态
        participation.setStatus(3);
        participationRepository.updateById(participation);

        return submission.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkinEvent(Long eventId, Long userId) {
        SysEvent event = getEventOrThrow(eventId);

        // 校验活动类型（只有线下活动可以签到）
        if (event.getEventType() != 2) {
            throw new BusinessException(ResultCode.FAIL, "该活动不支持签到");
        }

        // 查询报名记录
        LambdaQueryWrapper<SysEventParticipation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysEventParticipation::getEventId, eventId)
                .eq(SysEventParticipation::getUserId, userId)
                .ne(SysEventParticipation::getStatus, 5);
        SysEventParticipation participation = participationRepository.selectOne(wrapper);

        if (participation == null) {
            throw new BusinessException(ResultCode.FAIL, "请先报名参加活动");
        }

        if (participation.getStatus() == 2) {
            throw new BusinessException(ResultCode.FAIL, "您已签到");
        }

        // 更新签到状态
        participation.setStatus(2);
        participation.setCheckinTime(LocalDateTime.now());
        participation.setResult("已现场签到");
        participationRepository.updateById(participation);
    }

    @Override
    public PageResult<EventSubmissionVO> getEventSubmissions(Long eventId, Long userId, EventQueryRequest request) {
        // 验证活动存在
        getEventOrThrow(eventId);

        int pageNum = request.getPage() != null ? request.getPage() : request.getPageNum();
        int pageSize = request.getSize() != null ? request.getSize() : request.getPageSize();

        Page<SysEventSubmission> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<SysEventSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysEventSubmission::getEventId, eventId)
                .eq(SysEventSubmission::getStatus, 2) // 只显示已审核通过的作品
                .eq(SysEventSubmission::getIsDelete, 0)
                .orderByDesc(SysEventSubmission::getCreateTime);

        Page<SysEventSubmission> result = submissionRepository.selectPage(page, wrapper);

        List<EventSubmissionVO> records = result.getRecords().stream()
                .map(s -> convertToSubmissionVO(s, userId))
                .collect(Collectors.toList());

        return PageResult.<EventSubmissionVO>builder()
                .records(records)
                .total(result.getTotal())
                .pageNum((int) result.getCurrent())
                .pageSize((int) result.getSize())
                .pages((int) result.getPages())
                .build();
    }

    @Override
    public EventSubmissionVO getMySubmission(Long eventId, Long userId) {
        // 验证活动存在
        getEventOrThrow(eventId);

        // 查询用户在该活动的作品（排除已删除的，取最新的一条）
        LambdaQueryWrapper<SysEventSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysEventSubmission::getEventId, eventId)
                .eq(SysEventSubmission::getUserId, userId)
                .eq(SysEventSubmission::getIsDelete, 0)
                .orderByDesc(SysEventSubmission::getCreateTime)
                .last("LIMIT 1");
        SysEventSubmission submission = submissionRepository.selectOne(wrapper);

        if (submission == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "您尚未提交作品");
        }

        return convertToSubmissionDetailVO(submission, userId, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSubmission(Long submissionId, Long userId, EventSubmissionCreateRequest request) {
        SysEventSubmission submission = submissionRepository.selectById(submissionId);

        if (submission == null || Objects.equals(submission.getIsDelete(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "作品不存在");
        }

        // 验证是否是自己的作品
        if (!submission.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权修改此作品");
        }

        // 验证活动状态（已结束或评审中不可修改）
        SysEvent event = eventRepository.selectById(submission.getEventId());
        if (event != null && (event.getStatus() == 3 || event.getStatus() == 4)) {
            throw new BusinessException(ResultCode.FAIL, "活动已进入评审或已结束，无法修改作品");
        }

        // 更新作品信息
        submission.setTitle(request.getTitle());
        submission.setDescription(request.getDescription());
        if (request.getFileUrls() != null && !request.getFileUrls().isEmpty()) {
            submission.setFileUrls(String.join(",", request.getFileUrls()));
        }
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            submission.setImageUrls(String.join(",", request.getImageUrls()));
        }

        submissionRepository.updateById(submission);
    }

    // ==================== 作品互动接口 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeSubmission(Long submissionId, Long userId) {
        SysEventSubmission submission = getSubmissionOrThrow(submissionId);

        // 检查是否已点赞
        LambdaQueryWrapper<SysEventSubmissionLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysEventSubmissionLike::getSubmissionId, submissionId)
                .eq(SysEventSubmissionLike::getUserId, userId);
        if (submissionLikeRepository.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.FAIL, "您已点赞过该作品");
        }

        // 创建点赞记录
        SysEventSubmissionLike like = new SysEventSubmissionLike();
        like.setSubmissionId(submissionId);
        like.setUserId(userId);
        like.setCreateTime(LocalDateTime.now());
        submissionLikeRepository.insert(like);

        // 更新点赞数
        submission.setLikeCount(submission.getLikeCount() == null ? 1 : submission.getLikeCount() + 1);
        submissionRepository.updateById(submission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeSubmission(Long submissionId, Long userId) {
        SysEventSubmission submission = getSubmissionOrThrow(submissionId);

        // 删除点赞记录
        LambdaQueryWrapper<SysEventSubmissionLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysEventSubmissionLike::getSubmissionId, submissionId)
                .eq(SysEventSubmissionLike::getUserId, userId);
        int deleted = submissionLikeRepository.delete(wrapper);

        if (deleted > 0) {
            // 更新点赞数
            submission.setLikeCount(Math.max(0, (submission.getLikeCount() == null ? 0 : submission.getLikeCount()) - 1));
            submissionRepository.updateById(submission);
        }
    }

    @Override
    public EventSubmissionVO getSubmissionDetail(Long submissionId, Long userId) {
        SysEventSubmission submission = getSubmissionOrThrow(submissionId);

        // 非作者只能查看已通过审核的作品
        boolean isMine = userId != null && userId.equals(submission.getUserId());
        if (!isMine && submission.getStatus() != 2) {
            throw new BusinessException(ResultCode.NOT_FOUND, "作品不存在");
        }

        EventSubmissionVO vo = convertToSubmissionDetailVO(submission, userId, isMine);

        // 查询是否已点赞
        if (userId != null) {
            LambdaQueryWrapper<SysEventSubmissionLike> likeWrapper = new LambdaQueryWrapper<>();
            likeWrapper.eq(SysEventSubmissionLike::getSubmissionId, submissionId)
                    .eq(SysEventSubmissionLike::getUserId, userId);
            vo.setIsLiked(submissionLikeRepository.selectCount(likeWrapper) > 0);
        } else {
            vo.setIsLiked(false);
        }

        // 设置点赞数和评论数
        vo.setLikeCount(submission.getLikeCount() != null ? submission.getLikeCount() : 0);
        vo.setCommentCount(submission.getCommentCount() != null ? submission.getCommentCount() : 0);

        return vo;
    }

    @Override
    public PageResult<SubmissionCommentVO> getSubmissionComments(Long submissionId, Long userId, EventQueryRequest request) {
        // 验证作品存在
        getSubmissionOrThrow(submissionId);

        int pageNum = request.getPage() != null ? request.getPage() : request.getPageNum();
        int pageSize = request.getSize() != null ? request.getSize() : request.getPageSize();

        Page<SysEventSubmissionComment> page = new Page<>(pageNum, pageSize);

        // 查询一级评论
        LambdaQueryWrapper<SysEventSubmissionComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysEventSubmissionComment::getSubmissionId, submissionId)
                .eq(SysEventSubmissionComment::getIsDelete, 0)
                .isNull(SysEventSubmissionComment::getParentId)
                .orderByDesc(SysEventSubmissionComment::getCreateTime);

        Page<SysEventSubmissionComment> result = submissionCommentRepository.selectPage(page, wrapper);

        List<SubmissionCommentVO> records = result.getRecords().stream()
                .map(c -> convertToCommentVO(c, userId))
                .collect(Collectors.toList());

        return PageResult.<SubmissionCommentVO>builder()
                .records(records)
                .total(result.getTotal())
                .pageNum((int) result.getCurrent())
                .pageSize((int) result.getSize())
                .pages((int) result.getPages())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long commentSubmission(SubmissionCommentRequest request, Long userId) {
        // 验证作品存在
        SysEventSubmission submission = getSubmissionOrThrow(request.getSubmissionId());

        // 验证父评论
        Long parentId = request.getParentId();
        Long replyToUserId = request.getReplyToUserId();
        if (parentId != null) {
            SysEventSubmissionComment parent = submissionCommentRepository.selectById(parentId);
            if (parent == null || parent.getIsDelete() == 1) {
                throw new BusinessException(ResultCode.NOT_FOUND, "回复的评论不存在");
            }
            if (!parent.getSubmissionId().equals(request.getSubmissionId())) {
                throw new BusinessException(ResultCode.FAIL, "评论不属于该作品");
            }
        }

        SysEventSubmissionComment comment = new SysEventSubmissionComment();
        comment.setSubmissionId(request.getSubmissionId());
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setReplyToUserId(replyToUserId);
        comment.setContent(request.getContent());
        comment.setLikeCount(0);
        comment.setIsDelete(0);
        comment.setCreateTime(LocalDateTime.now());
        submissionCommentRepository.insert(comment);

        // 更新评论数
        submission.setCommentCount(submission.getCommentCount() == null ? 1 : submission.getCommentCount() + 1);
        submissionRepository.updateById(submission);

        return comment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long userId) {
        SysEventSubmissionComment comment = submissionCommentRepository.selectById(commentId);
        if (comment == null || comment.getIsDelete() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }

        // 只能删除自己的评论
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除此评论");
        }

        // 逻辑删除
        comment.setIsDelete(1);
        comment.setUpdateTime(LocalDateTime.now());
        submissionCommentRepository.updateById(comment);

        // 更新作品评论数
        SysEventSubmission submission = submissionRepository.selectById(comment.getSubmissionId());
        if (submission != null) {
            submission.setCommentCount(Math.max(0, (submission.getCommentCount() == null ? 0 : submission.getCommentCount()) - 1));
            submissionRepository.updateById(submission);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteComment(Long commentId) {
        SysEventSubmissionComment comment = submissionCommentRepository.selectById(commentId);
        if (comment == null || comment.getIsDelete() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }

        // 逻辑删除
        comment.setIsDelete(1);
        comment.setUpdateTime(LocalDateTime.now());
        submissionCommentRepository.updateById(comment);

        // 更新作品评论数
        SysEventSubmission submission = submissionRepository.selectById(comment.getSubmissionId());
        if (submission != null) {
            submission.setCommentCount(Math.max(0, (submission.getCommentCount() == null ? 0 : submission.getCommentCount()) - 1));
            submissionRepository.updateById(submission);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(Long commentId, Long userId) {
        SysEventSubmissionComment comment = submissionCommentRepository.selectById(commentId);
        if (comment == null || comment.getIsDelete() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }

        // 检查是否已点赞
        LambdaQueryWrapper<SysEventCommentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysEventCommentLike::getCommentId, commentId)
                .eq(SysEventCommentLike::getUserId, userId);
        if (commentLikeRepository.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.FAIL, "您已点赞过该评论");
        }

        // 创建点赞记录
        SysEventCommentLike like = new SysEventCommentLike();
        like.setCommentId(commentId);
        like.setUserId(userId);
        like.setCreateTime(LocalDateTime.now());
        commentLikeRepository.insert(like);

        // 更新点赞数
        comment.setLikeCount(comment.getLikeCount() == null ? 1 : comment.getLikeCount() + 1);
        submissionCommentRepository.updateById(comment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeComment(Long commentId, Long userId) {
        SysEventSubmissionComment comment = submissionCommentRepository.selectById(commentId);
        if (comment == null) {
            return;
        }

        // 删除点赞记录
        LambdaQueryWrapper<SysEventCommentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysEventCommentLike::getCommentId, commentId)
                .eq(SysEventCommentLike::getUserId, userId);
        int deleted = commentLikeRepository.delete(wrapper);

        if (deleted > 0) {
            comment.setLikeCount(Math.max(0, (comment.getLikeCount() == null ? 0 : comment.getLikeCount()) - 1));
            submissionCommentRepository.updateById(comment);
        }
    }

    // ==================== 管理端作品审核接口 ====================

    @Override
    public PageResult<EventSubmissionVO> getAdminSubmissions(Long eventId, EventQueryRequest request) {
        // 验证活动存在
        getEventOrThrow(eventId);

        int pageNum = request.getPage() != null ? request.getPage() : request.getPageNum();
        int pageSize = request.getSize() != null ? request.getSize() : request.getPageSize();

        Page<SysEventSubmission> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<SysEventSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysEventSubmission::getEventId, eventId)
                .eq(SysEventSubmission::getIsDelete, 0);

        // 管理端可按状态筛选
        if (request.getStatus() != null) {
            try {
                wrapper.eq(SysEventSubmission::getStatus, Integer.parseInt(request.getStatus()));
            } catch (NumberFormatException ignored) {}
        }

        wrapper.orderByDesc(SysEventSubmission::getCreateTime);

        Page<SysEventSubmission> result = submissionRepository.selectPage(page, wrapper);

        List<EventSubmissionVO> records = result.getRecords().stream()
                .map(s -> convertToSubmissionDetailVO(s, null, true))
                .collect(Collectors.toList());

        return PageResult.<EventSubmissionVO>builder()
                .records(records)
                .total(result.getTotal())
                .pageNum((int) result.getCurrent())
                .pageSize((int) result.getSize())
                .pages((int) result.getPages())
                .build();
    }

    @Override
    public PageResult<EventSubmissionVO> getAllAdminSubmissions(EventQueryRequest request) {
        int pageNum = request.getPage() != null ? request.getPage() : request.getPageNum();
        int pageSize = request.getSize() != null ? request.getSize() : request.getPageSize();

        Page<SysEventSubmission> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<SysEventSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysEventSubmission::getIsDelete, 0);

        // 管理端可按状态筛选
        if (request.getStatus() != null) {
            try {
                wrapper.eq(SysEventSubmission::getStatus, Integer.parseInt(request.getStatus()));
            } catch (NumberFormatException ignored) {}
        }

        // 可按活动ID筛选
        if (request.getEventId() != null) {
            wrapper.eq(SysEventSubmission::getEventId, request.getEventId());
        }

        wrapper.orderByDesc(SysEventSubmission::getCreateTime);

        Page<SysEventSubmission> result = submissionRepository.selectPage(page, wrapper);

        List<EventSubmissionVO> records = result.getRecords().stream()
                .map(s -> convertToSubmissionDetailVO(s, null, true))
                .collect(Collectors.toList());

        return PageResult.<EventSubmissionVO>builder()
                .records(records)
                .total(result.getTotal())
                .pageNum((int) result.getCurrent())
                .pageSize((int) result.getSize())
                .pages((int) result.getPages())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewSubmission(SubmissionReviewRequest request, Long reviewerId) {
        SysEventSubmission submission = getSubmissionOrThrow(request.getSubmissionId());

        if (submission.getStatus() != 1) {
            throw new BusinessException(ResultCode.FAIL, "只能审核待审核状态的作品");
        }

        if (request.getStatus() != 2 && request.getStatus() != 3) {
            throw new BusinessException(ResultCode.FAIL, "审核状态只能是2(通过)或3(拒绝)");
        }

        submission.setStatus(request.getStatus());
        submission.setReviewRemark(request.getReviewRemark());
        submission.setScore(request.getScore());
        submission.setReviewerId(reviewerId);
        submission.setReviewTime(LocalDateTime.now());
        submissionRepository.updateById(submission);
    }

    // ==================== 私有方法 ====================

    private SysEventSubmission getSubmissionOrThrow(Long id) {
        SysEventSubmission submission = submissionRepository.selectById(id);
        if (submission == null || Objects.equals(submission.getIsDelete(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "作品不存在");
        }
        return submission;
    }

    private SysEvent getEventOrThrow(Long id) {
        SysEvent event = eventRepository.selectById(id);
        if (event == null || Objects.equals(event.getIsDelete(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "活动不存在");
        }
        return event;
    }

    private void validateEventTime(LocalDateTime startTime, LocalDateTime endTime,
                                    LocalDateTime signupStart, LocalDateTime signupEnd) {
        if (startTime.isAfter(endTime)) {
            throw new BusinessException(ResultCode.FAIL, "开始时间不能晚于结束时间");
        }
        if (signupStart != null && signupEnd != null && signupStart.isAfter(signupEnd)) {
            throw new BusinessException(ResultCode.FAIL, "报名开始时间不能晚于报名截止时间");
        }
    }

    private Integer calculateInitStatus(EventCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();
        if (request.getSignupStart() != null && now.isBefore(request.getSignupStart())) {
            return 0; // 未开始
        }
        if (request.getSignupEnd() != null && now.isBefore(request.getSignupEnd())) {
            return 1; // 报名中
        }
        if (now.isBefore(request.getStartTime())) {
            return 0; // 未开始
        }
        if (now.isBefore(request.getEndTime())) {
            return 2; // 进行中
        }
        return 4; // 已结束
    }

    private void saveRewards(Long eventId, List<EventRewardRequest> rewards) {
        if (rewards == null || rewards.isEmpty()) {
            return;
        }
        int order = 1;
        for (EventRewardRequest rewardReq : rewards) {
            SysEventReward reward = new SysEventReward();
            reward.setEventId(eventId);
            reward.setRankName(rewardReq.getRankName());
            reward.setRankOrder(rewardReq.getRankOrder() != null ? rewardReq.getRankOrder() : order++);
            reward.setWinnerCount(rewardReq.getWinnerCount() != null ? rewardReq.getWinnerCount() : 1);
            reward.setPrizeContent(rewardReq.getPrizeContent());
            reward.setPoints(rewardReq.getPoints() != null ? rewardReq.getPoints() : 0);
            reward.setIsDelete(0);
            rewardRepository.insert(reward);
        }
    }

    // ==================== 管理端报名管理接口 ====================

    @Override
    public PageResult<EventParticipationVO> getAdminParticipations(Long eventId, EventQueryRequest request) {
        Page<SysEventParticipation> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<SysEventParticipation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(eventId != null, SysEventParticipation::getEventId, eventId);
        wrapper.eq(request.getStatus() != null, SysEventParticipation::getStatus, request.getStatus());
        wrapper.orderByDesc(SysEventParticipation::getSignupTime);

        Page<SysEventParticipation> result = participationRepository.selectPage(page, wrapper);

        List<EventParticipationVO> records = result.getRecords().stream()
                .map(p -> convertToParticipationVO(p, null))
                .collect(Collectors.toList());

        return PageResult.<EventParticipationVO>builder()
                .records(records)
                .total(result.getTotal())
                .pageNum((int) result.getCurrent())
                .pageSize((int) result.getSize())
                .pages((int) result.getPages())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateParticipationStatus(ParticipationStatusUpdateRequest request) {
        SysEventParticipation participation = participationRepository.selectById(request.getId());
        if (participation == null || participation.getIsDelete() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "参与记录不存在");
        }

        participation.setStatus(request.getStatus());
        participation.setAwardRank(request.getAwardRank());
        participation.setResult(request.getResult());
        participation.setUpdateTime(LocalDateTime.now());

        // 如果状态为已签到，设置签到时间
        if (request.getStatus() == 2 && participation.getCheckinTime() == null) {
            participation.setCheckinTime(LocalDateTime.now());
        }

        participationRepository.updateById(participation);
    }

    private EventVO convertToEventVO(SysEvent event) {
        EventVO vo = new EventVO();
        vo.setId(event.getId());
        vo.setTitle(event.getTitle());
        vo.setBannerUrl(event.getBannerUrl());
        vo.setEventType(event.getEventType());
        vo.setEventTypeName(getEventTypeName(event.getEventType()));
        vo.setStartTime(event.getStartTime());
        vo.setEndTime(event.getEndTime());
        vo.setCurrentParticipants(event.getCurrentParticipants());
        vo.setMaxParticipants(event.getMaxParticipants());
        vo.setStatus(event.getStatus());
        vo.setStatusName(getStatusName(event.getStatus()));
        vo.setCreateTime(event.getCreateTime());
        return vo;
    }

    private EventListVO convertToEventListVO(SysEvent event) {
        EventListVO vo = new EventListVO();
        vo.setId(event.getId());
        vo.setTitle(event.getTitle());
        vo.setBannerUrl(event.getBannerUrl());
        vo.setEventType(event.getEventType());
        vo.setEventTypeName(getEventTypeName(event.getEventType()));
        vo.setStartTime(event.getStartTime());
        vo.setEndTime(event.getEndTime());
        vo.setLocation(event.getLocation());
        vo.setStatus(event.getStatus());
        vo.setStatusName(getStatusName(event.getStatus()));
        vo.setCurrentParticipants(event.getCurrentParticipants());
        return vo;
    }

    private EventParticipationVO convertToParticipationVO(SysEventParticipation p, Long userId) {
        EventParticipationVO vo = new EventParticipationVO();
        vo.setId(p.getId());
        vo.setEventId(p.getEventId());
        vo.setUserId(p.getUserId());
        vo.setSignupTime(p.getSignupTime());
        vo.setCheckinTime(p.getCheckinTime());
        vo.setStatus(p.getStatus());
        vo.setStatusName(getParticipationStatusName(p.getStatus()));
        vo.setAwardRank(p.getAwardRank());
        vo.setPointsSent(p.getPointsSent());
        vo.setPointsSentTime(p.getPointsSentTime());
        vo.setResult(p.getResult());

        // 查询用户信息
        SysUser user = userRepository.selectById(p.getUserId());
        if (user != null) {
            vo.setUserName(user.getNickname());
            vo.setUserAvatar(user.getAvatar());
        }

        // 查询活动信息
        SysEvent event = eventRepository.selectById(p.getEventId());
        if (event != null) {
            vo.setEventTitle(event.getTitle());
            vo.setEventBanner(event.getBannerUrl());
            vo.setEventType(event.getEventType());
            vo.setEventTypeName(getEventTypeName(event.getEventType()));
            vo.setEventStatus(event.getStatus());
            vo.setEventStatusName(getStatusName(event.getStatus()));
        }
        return vo;
    }

    private String getEventTypeName(Integer eventType) {
        if (eventType == null) return "其他";
        return switch (eventType) {
            case 1 -> "设计竞赛";
            case 2 -> "线下活动";
            default -> "其他";
        };
    }

    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "未开始";
            case 1 -> "报名中";
            case 2 -> "进行中";
            case 3 -> "评审中";
            case 4 -> "已结束";
            default -> "未知";
        };
    }

    private String getParticipationStatusName(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 1 -> "已报名";
            case 2 -> "已签到";
            case 3 -> "已提交作品";
            case 4 -> "已获奖";
            case 5 -> "已取消";
            default -> "未知";
        };
    }

    /**
     * 转换字符串状态参数为状态码
     * @param status 字符串状态: upcoming, ongoing, ended
     * @return 状态码: 0-未开始, -1-进行中(1,2,3), 4-已结束
     */
    private Integer convertStatusParam(String status) {
        if (!StringUtils.hasText(status)) return null;
        return switch (status.toLowerCase()) {
            case "upcoming" -> 0;
            case "ongoing" -> -1; // 特殊标记，表示进行中(包含报名中、进行中、评审中)
            case "ended" -> 4;
            default -> null;
        };
    }

    /**
     * 解析状态值(管理端使用)
     * @param status 状态字符串，可以是数字或英文
     * @return 状态码
     */
    private Integer parseStatusValue(String status) {
        if (!StringUtils.hasText(status)) return null;
        try {
            return Integer.parseInt(status);
        } catch (NumberFormatException e) {
            return convertStatusParam(status);
        }
    }

    private EventSubmissionVO convertToSubmissionVO(SysEventSubmission submission, Long currentUserId) {
        return convertToSubmissionDetailVO(submission, currentUserId, false);
    }

    private EventSubmissionVO convertToSubmissionDetailVO(SysEventSubmission submission, Long currentUserId, boolean includeFiles) {
        EventSubmissionVO vo = new EventSubmissionVO();
        vo.setId(submission.getId());
        vo.setTitle(submission.getTitle());
        vo.setDescription(submission.getDescription());
        vo.setStatus(submission.getStatus());
        vo.setStatusName(getSubmissionStatusName(submission.getStatus()));
        vo.setScore(submission.getScore());
        vo.setCreateTime(submission.getCreateTime());
        vo.setAuthorId(submission.getUserId());

        // 活动ID
        vo.setEventId(submission.getEventId());

        // 点赞数和评论数
        vo.setLikeCount(submission.getLikeCount() != null ? submission.getLikeCount() : 0);
        vo.setCommentCount(submission.getCommentCount() != null ? submission.getCommentCount() : 0);

        // 图片URL（公开可见）
        if (StringUtils.hasText(submission.getImageUrls())) {
            vo.setImageUrls(List.of(submission.getImageUrls().split(",")));
        }

        // 是否为当前用户的作品
        boolean isMine = currentUserId != null && currentUserId.equals(submission.getUserId());
        vo.setIsMine(isMine);

        // 模型文件URL（管理端或自己的作品可见）
        if (includeFiles && StringUtils.hasText(submission.getFileUrls())) {
            vo.setFileUrls(List.of(submission.getFileUrls().split(",")));
        }

        // 查询作者信息
        SysUser author = userRepository.selectById(submission.getUserId());
        if (author != null) {
            vo.setAuthorName(author.getNickname());
            vo.setAuthorAvatar(author.getAvatar());
        }

        return vo;
    }

    private String getSubmissionStatusName(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 1 -> "待审核";
            case 2 -> "已通过";
            case 3 -> "已拒绝";
            default -> "未知";
        };
    }

    private SubmissionCommentVO convertToCommentVO(SysEventSubmissionComment comment, Long currentUserId) {
        SubmissionCommentVO vo = new SubmissionCommentVO();
        vo.setId(comment.getId());
        vo.setUserId(comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setLikeCount(comment.getLikeCount() != null ? comment.getLikeCount() : 0);
        vo.setCreateTime(comment.getCreateTime());
        vo.setParentId(comment.getParentId());

        // 查询评论用户信息
        SysUser user = userRepository.selectById(comment.getUserId());
        if (user != null) {
            vo.setUserName(user.getNickname());
            vo.setUserAvatar(user.getAvatar());
        }

        // 查询回复的用户昵称
        if (comment.getReplyToUserId() != null) {
            SysUser replyToUser = userRepository.selectById(comment.getReplyToUserId());
            if (replyToUser != null) {
                vo.setReplyToUserName(replyToUser.getNickname());
            }
        }

        // 查询是否已点赞
        if (currentUserId != null) {
            LambdaQueryWrapper<SysEventCommentLike> likeWrapper = new LambdaQueryWrapper<>();
            likeWrapper.eq(SysEventCommentLike::getCommentId, comment.getId())
                    .eq(SysEventCommentLike::getUserId, currentUserId);
            vo.setIsLiked(commentLikeRepository.selectCount(likeWrapper) > 0);
        } else {
            vo.setIsLiked(false);
        }

        // 查询子评论
        LambdaQueryWrapper<SysEventSubmissionComment> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(SysEventSubmissionComment::getParentId, comment.getId())
                .eq(SysEventSubmissionComment::getIsDelete, 0)
                .orderByAsc(SysEventSubmissionComment::getCreateTime);
        List<SysEventSubmissionComment> children = submissionCommentRepository.selectList(childWrapper);
        if (!children.isEmpty()) {
            vo.setChildren(children.stream()
                    .map(c -> convertToCommentVO(c, currentUserId))
                    .collect(Collectors.toList()));
        }

        return vo;
    }

    // ==================== 管理端颁奖接口 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void awardPoints(AwardPointsRequest request) {
        SysEvent event = getEventOrThrow(request.getEventId());

        // 校验活动状态（只有已结束的活动可以颁奖）
        if (event.getStatus() != 4) {
            throw new BusinessException(ResultCode.FAIL, "只有已结束的活动才能颁奖");
        }

        // 获取活动的奖励配置（奖项 -> 积分映射）
        java.util.Map<String, Integer> rewardPointsMap = getRewardPointsMap(request.getEventId());

        for (Long participationId : request.getParticipationIds()) {
            SysEventParticipation participation = participationRepository.selectById(participationId);
            if (participation == null || participation.getIsDelete() == 1) {
                continue;
            }

            // 校验是否为获奖者
            if (participation.getStatus() != 4) {
                continue;
            }

            // 校验是否已发放
            if (Objects.equals(participation.getPointsSent(), 1)) {
                continue;
            }

            // 校验活动ID匹配
            if (!participation.getEventId().equals(request.getEventId())) {
                continue;
            }

            // 获取对应奖项的积分
            Integer points = rewardPointsMap.get(participation.getAwardRank());
            if (points == null || points <= 0) {
                continue;
            }

            // 生成业务流水号（使用参与记录ID保证幂等性）
            String bizNo = "EVENT_AWARD_" + participationId;

            // 发放积分
            String remark = String.format("活动[%s]获奖奖励[%s]", event.getTitle(), participation.getAwardRank());
            pointService.increase(participation.getUserId(), points, PointService.BIZ_EVENT_AWARD, bizNo, participationId, remark);

            // 更新发放状态
            participation.setPointsSent(1);
            participation.setPointsSentTime(LocalDateTime.now());
            participationRepository.updateById(participation);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AwardPointsResultVO awardAllWinners(Long eventId) {
        SysEvent event = getEventOrThrow(eventId);

        // 校验活动状态
        if (event.getStatus() != 4) {
            throw new BusinessException(ResultCode.FAIL, "只有已结束的活动才能颁奖");
        }

        // 获取活动的奖励配置
        java.util.Map<String, Integer> rewardPointsMap = getRewardPointsMap(eventId);

        // 查询所有获奖者（status=4）
        LambdaQueryWrapper<SysEventParticipation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysEventParticipation::getEventId, eventId)
                .eq(SysEventParticipation::getStatus, 4)
                .eq(SysEventParticipation::getIsDelete, 0);
        List<SysEventParticipation> winners = participationRepository.selectList(wrapper);

        int successCount = 0;
        int failCount = 0;
        int skippedCount = 0;
        int totalPoints = 0;

        for (SysEventParticipation participation : winners) {
            // 已发放的跳过
            if (Objects.equals(participation.getPointsSent(), 1)) {
                skippedCount++;
                continue;
            }

            // 获取对应奖项的积分
            Integer points = rewardPointsMap.get(participation.getAwardRank());
            if (points == null || points <= 0) {
                failCount++;
                continue;
            }

            try {
                // 生成业务流水号
                String bizNo = "EVENT_AWARD_" + participation.getId();

                // 发放积分
                String remark = String.format("活动[%s]获奖奖励[%s]", event.getTitle(), participation.getAwardRank());
                pointService.increase(participation.getUserId(), points, PointService.BIZ_EVENT_AWARD, bizNo, participation.getId(), remark);

                // 更新发放状态
                participation.setPointsSent(1);
                participation.setPointsSentTime(LocalDateTime.now());
                participationRepository.updateById(participation);

                successCount++;
                totalPoints += points;
            } catch (Exception e) {
                log.error("颁奖失败, participationId={}", participation.getId(), e);
                failCount++;
            }
        }

        return AwardPointsResultVO.builder()
                .successCount(successCount)
                .failCount(failCount)
                .skippedCount(skippedCount)
                .totalPoints(totalPoints)
                .build();
    }

    /**
     * 获取活动的奖项-积分映射
     */
    private java.util.Map<String, Integer> getRewardPointsMap(Long eventId) {
        List<SysEventReward> rewards = rewardRepository.selectList(
                new LambdaQueryWrapper<SysEventReward>()
                        .eq(SysEventReward::getEventId, eventId)
        );
        return rewards.stream()
                .filter(r -> r.getPoints() != null && r.getPoints() > 0)
                .collect(Collectors.toMap(SysEventReward::getRankName, SysEventReward::getPoints, (a, b) -> a));
    }
}
