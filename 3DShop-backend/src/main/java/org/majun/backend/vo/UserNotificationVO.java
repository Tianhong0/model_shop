package org.majun.backend.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserNotificationVO {

    private Long id;

    private String category;

    private String notificationType;

    private String title;

    private String content;

    private String coverUrl;

    private Long senderId;

    private String senderName;

    private Long bizId;

    private String bizNo;

    private String redirectUrl;

    private Integer popupRequired;

    private Integer isRead;

    private LocalDateTime createTime;
}