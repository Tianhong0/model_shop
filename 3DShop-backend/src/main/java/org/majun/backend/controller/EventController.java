package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.Result;
import org.majun.backend.dto.*;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.EventService;
import org.majun.backend.vo.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 活动赛事控制器
 */
@Tag(name = "活动赛事", description = "活动赛事管理接口")
@RestController
@RequestMapping("/api/event")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // ==================== 管理端接口 ====================

    @Operation(summary = "管理端活动分页")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/list")
    public Result<PageResult<EventVO>> adminList(@Valid @RequestBody EventQueryRequest request) {
        return Result.success(eventService.getAdminEventList(request));
    }

    @Operation(summary = "创建活动")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/create")
    public Result<Long> create(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody EventCreateRequest request) {
        return Result.success(eventService.createEvent(request, loginUser.getId()));
    }

    @Operation(summary = "更新活动")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/admin/update")
    public Result<Void> update(@Valid @RequestBody EventUpdateRequest request) {
        eventService.updateEvent(request);
        return Result.success();
    }

    @Operation(summary = "更新活动状态")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/admin/status")
    public Result<Void> updateStatus(@Valid @RequestBody EventStatusUpdateRequest request) {
        eventService.updateEventStatus(request);
        return Result.success();
    }

    @Operation(summary = "删除活动")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/admin/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return Result.success();
    }

    @Operation(summary = "活动详情(管理端)")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/admin/detail/{id}")
    public Result<EventDetailVO> adminDetail(@PathVariable Long id) {
        return Result.success(eventService.getEventDetail(id, null));
    }

    // ==================== 移动端接口 ====================

    @Operation(summary = "活动列表(移动端)")
    @PostMapping("/list")
    public Result<PageResult<EventListVO>> list(@Valid @RequestBody EventQueryRequest request) {
        return Result.success(eventService.getEventList(request));
    }

    @Operation(summary = "活动详情(移动端)")
    @GetMapping("/detail/{id}")
    public Result<EventDetailVO> detail(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginUser loginUser) {
        Long userId = loginUser != null ? loginUser.getId() : null;
        return Result.success(eventService.getEventDetail(id, userId));
    }

    @Operation(summary = "活动报名")
    @PostMapping("/signup/{id}")
    public Result<Void> signup(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginUser loginUser) {
        eventService.signupEvent(id, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "取消报名")
    @PostMapping("/cancel/{id}")
    public Result<Void> cancelSignup(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginUser loginUser) {
        eventService.cancelSignup(id, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "我参加的活动")
    @PostMapping("/my/participations")
    public Result<PageResult<EventParticipationVO>> myParticipations(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody EventQueryRequest request) {
        return Result.success(eventService.getMyParticipations(loginUser.getId(), request));
    }

    @Operation(summary = "上传作品(设计竞赛)")
    @PostMapping("/submission/create")
    public Result<Long> createSubmission(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody EventSubmissionCreateRequest request) {
        return Result.success(eventService.createSubmission(request, loginUser.getId()));
    }

    @Operation(summary = "签到(线下活动)")
    @PostMapping("/checkin/{id}")
    public Result<Void> checkin(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginUser loginUser) {
        eventService.checkinEvent(id, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "获取活动作品列表")
    @PostMapping("/submissions/{eventId}")
    public Result<PageResult<EventSubmissionVO>> getSubmissions(
            @PathVariable Long eventId,
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody EventQueryRequest request) {
        Long userId = loginUser != null ? loginUser.getId() : null;
        return Result.success(eventService.getEventSubmissions(eventId, userId, request));
    }

    @Operation(summary = "获取我在某活动的作品")
    @GetMapping("/my-submission/{eventId}")
    public Result<EventSubmissionVO> getMySubmission(
            @PathVariable Long eventId,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(eventService.getMySubmission(eventId, loginUser.getId()));
    }

    @Operation(summary = "更新作品")
    @PutMapping("/submission/update/{submissionId}")
    public Result<Void> updateSubmission(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody EventSubmissionCreateRequest request) {
        eventService.updateSubmission(submissionId, loginUser.getId(), request);
        return Result.success();
    }

    // ==================== 作品互动接口 ====================

    @Operation(summary = "作品详情")
    @GetMapping("/submission/{submissionId}")
    public Result<EventSubmissionVO> getSubmissionDetail(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal LoginUser loginUser) {
        Long userId = loginUser != null ? loginUser.getId() : null;
        return Result.success(eventService.getSubmissionDetail(submissionId, userId));
    }

    @Operation(summary = "点赞作品")
    @PostMapping("/submission/like/{submissionId}")
    public Result<Void> likeSubmission(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal LoginUser loginUser) {
        eventService.likeSubmission(submissionId, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "取消点赞作品")
    @DeleteMapping("/submission/like/{submissionId}")
    public Result<Void> unlikeSubmission(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal LoginUser loginUser) {
        eventService.unlikeSubmission(submissionId, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "获取作品评论列表")
    @PostMapping("/submission/comments/{submissionId}")
    public Result<PageResult<SubmissionCommentVO>> getSubmissionComments(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody EventQueryRequest request) {
        Long userId = loginUser != null ? loginUser.getId() : null;
        return Result.success(eventService.getSubmissionComments(submissionId, userId, request));
    }

    @Operation(summary = "评论作品")
    @PostMapping("/submission/comment")
    public Result<Long> commentSubmission(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody SubmissionCommentRequest request) {
        return Result.success(eventService.commentSubmission(request, loginUser.getId()));
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/submission/comment/{commentId}")
    public Result<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal LoginUser loginUser) {
        eventService.deleteComment(commentId, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "点赞评论")
    @PostMapping("/submission/comment/like/{commentId}")
    public Result<Void> likeComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal LoginUser loginUser) {
        eventService.likeComment(commentId, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "取消点赞评论")
    @DeleteMapping("/submission/comment/like/{commentId}")
    public Result<Void> unlikeComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal LoginUser loginUser) {
        eventService.unlikeComment(commentId, loginUser.getId());
        return Result.success();
    }

    // ==================== 管理端作品审核接口 ====================

    @Operation(summary = "管理端获取活动作品列表")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/submissions/{eventId}")
    public Result<PageResult<EventSubmissionVO>> getAdminSubmissions(
            @PathVariable Long eventId,
            @Valid @RequestBody EventQueryRequest request) {
        return Result.success(eventService.getAdminSubmissions(eventId, request));
    }

    @Operation(summary = "管理端获取所有作品列表")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/submissions/all")
    public Result<PageResult<EventSubmissionVO>> getAllAdminSubmissions(
            @Valid @RequestBody EventQueryRequest request) {
        return Result.success(eventService.getAllAdminSubmissions(request));
    }

    @Operation(summary = "审核作品")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/submission/review")
    public Result<Void> reviewSubmission(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody SubmissionReviewRequest request) {
        eventService.reviewSubmission(request, loginUser.getId());
        return Result.success();
    }

    @Operation(summary = "管理端获取作品评论列表")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/submission/comments/{submissionId}")
    public Result<PageResult<SubmissionCommentVO>> getAdminSubmissionComments(
            @PathVariable Long submissionId,
            @Valid @RequestBody EventQueryRequest request) {
        return Result.success(eventService.getSubmissionComments(submissionId, null, request));
    }

    @Operation(summary = "管理端删除评论")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/admin/submission/comment/{commentId}")
    public Result<Void> adminDeleteComment(@PathVariable Long commentId) {
        eventService.adminDeleteComment(commentId);
        return Result.success();
    }

    // ==================== 管理端报名管理接口 ====================

    @Operation(summary = "管理端获取活动报名列表")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/participations/{eventId}")
    public Result<PageResult<EventParticipationVO>> getAdminParticipations(
            @PathVariable Long eventId,
            @Valid @RequestBody EventQueryRequest request) {
        return Result.success(eventService.getAdminParticipations(eventId, request));
    }

    @Operation(summary = "管理端获取所有报名列表")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/participations/all")
    public Result<PageResult<EventParticipationVO>> getAllAdminParticipations(
            @Valid @RequestBody EventQueryRequest request) {
        return Result.success(eventService.getAdminParticipations(null, request));
    }

    @Operation(summary = "管理端更新参与状态")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/admin/participation/status")
    public Result<Void> updateParticipationStatus(@Valid @RequestBody ParticipationStatusUpdateRequest request) {
        eventService.updateParticipationStatus(request);
        return Result.success();
    }

    // ==================== 管理端颁奖接口 ====================

    @Operation(summary = "为指定获奖者颁发积分")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/award-points")
    public Result<Void> awardPoints(@Valid @RequestBody AwardPointsRequest request) {
        eventService.awardPoints(request);
        return Result.success();
    }

    @Operation(summary = "批量发放活动所有获奖者积分")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/award-all/{eventId}")
    public Result<AwardPointsResultVO> awardAllWinners(@PathVariable Long eventId) {
        return Result.success(eventService.awardAllWinners(eventId));
    }
}
