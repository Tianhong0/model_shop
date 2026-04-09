package org.majun.backend.service;

import org.majun.backend.dto.*;
import org.majun.backend.vo.*;

/**
 * 活动赛事服务接口
 */
public interface EventService {

    // ==================== 管理端接口 ====================

    /**
     * 管理端活动分页列表
     */
    PageResult<EventVO> getAdminEventList(EventQueryRequest request);

    /**
     * 创建活动
     */
    Long createEvent(EventCreateRequest request, Long createBy);

    /**
     * 更新活动
     */
    void updateEvent(EventUpdateRequest request);

    /**
     * 更新活动状态
     */
    void updateEventStatus(EventStatusUpdateRequest request);

    /**
     * 删除活动
     */
    void deleteEvent(Long id);

    // ==================== 移动端接口 ====================

    /**
     * 活动列表(移动端)
     */
    PageResult<EventListVO> getEventList(EventQueryRequest request);

    /**
     * 活动详情
     */
    EventDetailVO getEventDetail(Long id, Long userId);

    /**
     * 活动报名
     */
    void signupEvent(Long eventId, Long userId);

    /**
     * 取消报名
     */
    void cancelSignup(Long eventId, Long userId);

    /**
     * 我参加的活动
     */
    PageResult<EventParticipationVO> getMyParticipations(Long userId, EventQueryRequest request);

    /**
     * 上传作品
     */
    Long createSubmission(EventSubmissionCreateRequest request, Long userId);

    /**
     * 签到
     */
    void checkinEvent(Long eventId, Long userId);

    /**
     * 获取活动作品列表
     */
    PageResult<EventSubmissionVO> getEventSubmissions(Long eventId, Long userId, EventQueryRequest request);

    /**
     * 获取我在某活动的作品
     */
    EventSubmissionVO getMySubmission(Long eventId, Long userId);

    /**
     * 更新作品
     */
    void updateSubmission(Long submissionId, Long userId, EventSubmissionCreateRequest request);

    // ==================== 作品互动接口 ====================

    /**
     * 点赞作品
     */
    void likeSubmission(Long submissionId, Long userId);

    /**
     * 取消点赞作品
     */
    void unlikeSubmission(Long submissionId, Long userId);

    /**
     * 获取作品详情(包含评论)
     */
    EventSubmissionVO getSubmissionDetail(Long submissionId, Long userId);

    /**
     * 获取作品评论列表
     */
    PageResult<SubmissionCommentVO> getSubmissionComments(Long submissionId, Long userId, EventQueryRequest request);

    /**
     * 评论作品
     */
    Long commentSubmission(SubmissionCommentRequest request, Long userId);

    /**
     * 删除评论
     */
    void deleteComment(Long commentId, Long userId);

    /**
     * 点赞评论
     */
    void likeComment(Long commentId, Long userId);

    /**
     * 取消点赞评论
     */
    void unlikeComment(Long commentId, Long userId);

    // ==================== 管理端作品审核接口 ====================

    /**
     * 管理端获取活动作品列表
     */
    PageResult<EventSubmissionVO> getAdminSubmissions(Long eventId, EventQueryRequest request);

    /**
     * 管理端获取所有作品列表
     */
    PageResult<EventSubmissionVO> getAllAdminSubmissions(EventQueryRequest request);

    /**
     * 审核作品
     */
    void reviewSubmission(SubmissionReviewRequest request, Long reviewerId);

    /**
     * 管理端删除评论
     */
    void adminDeleteComment(Long commentId);

    // ==================== 管理端报名管理接口 ====================

    /**
     * 管理端获取活动报名列表
     */
    PageResult<EventParticipationVO> getAdminParticipations(Long eventId, EventQueryRequest request);

    /**
     * 管理端更新参与状态
     */
    void updateParticipationStatus(ParticipationStatusUpdateRequest request);

    // ==================== 管理端颁奖接口 ====================

    /**
     * 为指定获奖者颁发积分奖励
     * @param request 颁奖请求
     */
    void awardPoints(AwardPointsRequest request);

    /**
     * 批量为活动所有获奖者颁奖
     * @param eventId 活动ID
     * @return 颁奖结果统计
     */
    AwardPointsResultVO awardAllWinners(Long eventId);
}
