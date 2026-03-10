package org.majun.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserNotificationPopupAckRequest {

    private List<Long> ids;
}