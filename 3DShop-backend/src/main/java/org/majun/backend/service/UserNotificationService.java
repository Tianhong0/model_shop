package org.majun.backend.service;

import org.majun.backend.dto.UserNotificationCreateCommand;
import org.majun.backend.dto.UserNotificationPageRequest;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.UserNotificationUnreadSummaryVO;
import org.majun.backend.vo.UserNotificationVO;

import java.util.List;

/**
 * 用户通知服务接口
 */
public interface UserNotificationService {

    void createNotification(UserNotificationCreateCommand command);

    PageResult<UserNotificationVO> pageNotifications(UserNotificationPageRequest request, Long userId);

    UserNotificationUnreadSummaryVO getUnreadSummary(Long userId);

    void markRead(Long notificationId, Long userId);

    void markAllRead(Long userId, String category);

    List<UserNotificationVO> listPendingPopupNotifications(Long userId, Integer limit);

    void ackPopupNotifications(List<Long> ids, Long userId);
}