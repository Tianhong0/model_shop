package org.majun.backend.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UsedMessageVO {

    private Long id;
    private Long listingId;
    private Long senderId;
    private String senderRole;
    private String senderNickname;
    private Long counterpartId;
    private Integer messageType;
    private String content;
    private String attachments;
    private Integer isSystem;
    private LocalDateTime createTime;
}
