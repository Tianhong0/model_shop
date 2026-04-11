package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.ResultCode;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.dto.UserNotificationCreateCommand;
import org.majun.backend.dto.UserNotificationPageRequest;
import org.majun.backend.entity.UserNotification;
import org.majun.backend.repository.UserNotificationRepository;
import org.majun.backend.service.UserNotificationService;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.UserNotificationUnreadSummaryVO;
import org.majun.backend.vo.UserNotificationVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
/**
 * 用户通知服务实现
 */
public class UserNotificationServiceImpl implements UserNotificationService {

    public static final String CATEGORY_TRADE = "TRADE";
    public static final String CATEGORY_LIKE = "LIKE";
    public static final String CATEGORY_LOGISTICS = "LOGISTICS";

    public static final String TYPE_COMMUNITY_POST_LIKE = "COMMUNITY_POST_LIKE";
    public static final String TYPE_COMMUNITY_REPLY_LIKE = "COMMUNITY_REPLY_LIKE";
    public static final String TYPE_ORDER_COMMENT_LIKE = "ORDER_COMMENT_LIKE";
    public static final String TYPE_ORDER_COMMENT_REPLY_LIKE = "ORDER_COMMENT_REPLY_LIKE";
    public static final String TYPE_MALL_DELIVERY = "MALL_DELIVERY";
    public static final String TYPE_USED_CHAT = "USED_CHAT";
    public static final String TYPE_USED_BARGAIN = "USED_BARGAIN";
    public static final String TYPE_USED_DELIVERY = "USED_DELIVERY";
    public static final String TYPE_USED_TRADE = "USED_TRADE";

    private final UserNotificationRepository userNotificationRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createNotification(UserNotificationCreateCommand command) {
        if (command == null || command.getUserId() == null || !StringUtils.hasText(command.getTitle())) {
            return;
        }
        UserNotification notification = new UserNotification();
        notification.setUserId(command.getUserId());
        notification.setCategory(StringUtils.hasText(command.getCategory()) ? command.getCategory().trim() : CATEGORY_TRADE);
        notification.setNotificationType(trim(command.getNotificationType()));
        notification.setTitle(trim(command.getTitle()));
        notification.setContent(trim(command.getContent()));
        notification.setCoverUrl(trim(command.getCoverUrl()));
        notification.setSenderId(command.getSenderId());
        notification.setSenderName(trim(command.getSenderName()));
        notification.setBizId(command.getBizId());
        notification.setBizNo(trim(command.getBizNo()));
        notification.setRedirectUrl(trim(command.getRedirectUrl()));
        notification.setPopupRequired(Boolean.TRUE.equals(command.getPopupRequired()) ? 1 : 0);
        notification.setPopupPushed(0);
        notification.setIsRead(0);
        notification.setExtJson(trim(command.getExtJson()));
        notification.setIsDelete(0);
        userNotificationRepository.insert(notification);
    }

    @Override
    public PageResult<UserNotificationVO> pageNotifications(UserNotificationPageRequest request, Long userId) {
        UserNotificationPageRequest safeRequest = request == null ? new UserNotificationPageRequest() : request;
        Page<UserNotification> page = new Page<>(safeRequest.getPageNum(), safeRequest.getPageSize());
        LambdaQueryWrapper<UserNotification> wrapper = baseUserWrapper(userId)
                .orderByDesc(UserNotification::getCreateTime);
        if (Boolean.TRUE.equals(safeRequest.getUnreadOnly())) {
            wrapper.eq(UserNotification::getIsRead, 0);
        }
        if (StringUtils.hasText(safeRequest.getCategory()) && !"ALL".equalsIgnoreCase(safeRequest.getCategory().trim())) {
            wrapper.eq(UserNotification::getCategory, safeRequest.getCategory().trim().toUpperCase());
        }
        Page<UserNotification> result = userNotificationRepository.selectPage(page, wrapper);
        List<UserNotificationVO> records = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.<UserNotificationVO>builder()
                .records(records)
                .total(result.getTotal())
                .pageNum((int) result.getCurrent())
                .pageSize((int) result.getSize())
                .pages((int) result.getPages())
                .build();
    }

    @Override
    public UserNotificationUnreadSummaryVO getUnreadSummary(Long userId) {
        UserNotificationUnreadSummaryVO vo = new UserNotificationUnreadSummaryVO();
        vo.setTotalUnread(countUnread(userId, null));
        vo.setTradeUnread(countUnread(userId, CATEGORY_TRADE));
        vo.setLikeUnread(countUnread(userId, CATEGORY_LIKE));
        vo.setLogisticsUnread(countUnread(userId, CATEGORY_LOGISTICS));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long notificationId, Long userId) {
        UserNotification notification = userNotificationRepository.selectOne(baseUserWrapper(userId)
                .eq(UserNotification::getId, notificationId)
                .last("limit 1"));
        if (notification == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "消息不存在");
        }
        if (Objects.equals(notification.getIsRead(), 1)) {
            return;
        }
        notification.setIsRead(1);
        userNotificationRepository.updateById(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long userId, String category) {
        LambdaUpdateWrapper<UserNotification> wrapper = new LambdaUpdateWrapper<UserNotification>()
                .eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getIsDelete, 0)
                .eq(UserNotification::getIsRead, 0)
                .set(UserNotification::getIsRead, 1);
        if (StringUtils.hasText(category) && !"ALL".equalsIgnoreCase(category.trim())) {
            wrapper.eq(UserNotification::getCategory, category.trim().toUpperCase());
        }
        userNotificationRepository.update(null, wrapper);
    }

    @Override
    public List<UserNotificationVO> listPendingPopupNotifications(Long userId, Integer limit) {
        int safeLimit = limit == null || limit <= 0 ? 10 : Math.min(limit, 20);
        List<UserNotification> list = userNotificationRepository.selectList(baseUserWrapper(userId)
                .eq(UserNotification::getPopupRequired, 1)
                .eq(UserNotification::getPopupPushed, 0)
                .orderByAsc(UserNotification::getCreateTime)
                .last("limit " + safeLimit));
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ackPopupNotifications(List<Long> ids, Long userId) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        userNotificationRepository.update(null, new LambdaUpdateWrapper<UserNotification>()
                .eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getIsDelete, 0)
                .in(UserNotification::getId, ids)
                .set(UserNotification::getPopupPushed, 1));
    }

    private Long countUnread(Long userId, String category) {
        LambdaQueryWrapper<UserNotification> wrapper = baseUserWrapper(userId)
                .eq(UserNotification::getIsRead, 0);
        if (StringUtils.hasText(category)) {
            wrapper.eq(UserNotification::getCategory, category);
        }
        return userNotificationRepository.selectCount(wrapper);
    }

    private LambdaQueryWrapper<UserNotification> baseUserWrapper(Long userId) {
        return new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getIsDelete, 0);
    }

    private UserNotificationVO toVO(UserNotification item) {
        UserNotificationVO vo = new UserNotificationVO();
        vo.setId(item.getId());
        vo.setCategory(item.getCategory());
        vo.setNotificationType(item.getNotificationType());
        vo.setTitle(item.getTitle());
        vo.setContent(item.getContent());
        vo.setCoverUrl(item.getCoverUrl());
        vo.setSenderId(item.getSenderId());
        vo.setSenderName(item.getSenderName());
        vo.setBizId(item.getBizId());
        vo.setBizNo(item.getBizNo());
        vo.setRedirectUrl(item.getRedirectUrl());
        vo.setPopupRequired(item.getPopupRequired());
        vo.setIsRead(item.getIsRead());
        vo.setCreateTime(item.getCreateTime());
        return vo;
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}