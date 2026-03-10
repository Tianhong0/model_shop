package org.majun.backend.vo;

import lombok.Data;

@Data
public class UserNotificationUnreadSummaryVO {

    private Long totalUnread;

    private Long tradeUnread;

    private Long likeUnread;

    private Long logisticsUnread;
}