package org.majun.backend.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsMessageVO {

    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderRole;
    private String senderNickname;
    private String senderAvatar;
    private Integer messageType;
    private String content;
    private String attachments;
    private Boolean isRead;
    private LocalDateTime createTime;
}
