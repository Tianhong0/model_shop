package org.majun.backend.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UsedMessageSessionVO {

    private Long counterpartId;
    private String counterpartNickname;
    private String counterpartAvatar;
    private String lastMessage;
    private Integer lastMessageType;
    private LocalDateTime lastMessageTime;
}
