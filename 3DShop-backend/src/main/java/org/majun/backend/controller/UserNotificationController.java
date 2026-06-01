package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.Result;
import org.majun.backend.dto.UserNotificationPageRequest;
import org.majun.backend.dto.UserNotificationPopupAckRequest;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.UserNotificationService;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.UserNotificationUnreadSummaryVO;
import org.majun.backend.vo.UserNotificationVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "User Notification", description = "用户消息中心")
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
/**
 * 用户通知控制器
 */
public class UserNotificationController {

    private final UserNotificationService userNotificationService;

    /**
     * 消息分页
     */
    @Operation(summary = "消息分页")
    @PostMapping("/page")
    public Result<PageResult<UserNotificationVO>> page(@AuthenticationPrincipal LoginUser loginUser,
                                                       @RequestBody(required = false) UserNotificationPageRequest request) {
        return Result.success(userNotificationService.pageNotifications(request, loginUser.getId()));
    }

    /**
     * 未读统计
     */
    @Operation(summary = "未读统计")
    @GetMapping("/unread/summary")
    public Result<UserNotificationUnreadSummaryVO> unreadSummary(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(userNotificationService.getUnreadSummary(loginUser.getId()));
    }

    /**
     * 标记单条已读
     */
    @Operation(summary = "标记单条已读")
    @PostMapping("/read/{notificationId}")
    public Result<Void> markRead(@AuthenticationPrincipal LoginUser loginUser,
                                 @PathVariable Long notificationId) {
        userNotificationService.markRead(notificationId, loginUser.getId());
        return Result.success();
    }

    /**
     * 全部标记已读
     */
    @Operation(summary = "全部标记已读")
    @PostMapping("/read/all")
    public Result<Void> markAllRead(@AuthenticationPrincipal LoginUser loginUser,
                                    @RequestParam(required = false) String category) {
        userNotificationService.markAllRead(loginUser.getId(), category);
        return Result.success();
    }

    /**
     * 待弹窗消息
     */
    @Operation(summary = "待弹窗消息")
    @GetMapping("/popup/pending")
    public Result<List<UserNotificationVO>> listPendingPopup(@AuthenticationPrincipal LoginUser loginUser,
                                                             @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(userNotificationService.listPendingPopupNotifications(loginUser.getId(), limit));
    }

    /**
     * 确认已弹窗
     */
    @Operation(summary = "确认已弹窗")
    @PostMapping("/popup/ack")
    public Result<Void> ackPopup(@AuthenticationPrincipal LoginUser loginUser,
                                 @RequestBody(required = false) UserNotificationPopupAckRequest request) {
        userNotificationService.ackPopupNotifications(request == null ? null : request.getIds(), loginUser.getId());
        return Result.success();
    }
}