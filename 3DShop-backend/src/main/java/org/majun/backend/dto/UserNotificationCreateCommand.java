package org.majun.backend.dto;

import lombok.Data;

@Data
public class UserNotificationCreateCommand {

    private Long userId;

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

    private Boolean popupRequired = false;

    private String extJson;
}