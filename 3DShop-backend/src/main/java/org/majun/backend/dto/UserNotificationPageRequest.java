package org.majun.backend.dto;

import lombok.Data;

@Data
public class UserNotificationPageRequest {

    private Integer pageNum = 1;

    private Integer pageSize = 20;

    private String category = "ALL";

    private Boolean unreadOnly = false;
}