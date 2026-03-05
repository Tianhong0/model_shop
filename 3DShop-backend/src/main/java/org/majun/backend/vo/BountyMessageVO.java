package org.majun.backend.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BountyMessageVO {

    private Long id;
    private Long taskId;
    private Long senderId;
    private String senderRole;
    private Integer messageType;
    private String content;
    private String attachments;
    private LocalDateTime createTime;
}
