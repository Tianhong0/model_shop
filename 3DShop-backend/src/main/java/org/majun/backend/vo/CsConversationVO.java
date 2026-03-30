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
public class CsConversationVO {

    private Long id;
    private String sessionNo;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private Long adminId;
    private String adminNickname;
    private Integer status;
    private String statusDesc;
    private String endReason;
    private LocalDateTime endTime;
    private Integer unreadUserCount;
    private Integer unreadAdminCount;
    private LocalDateTime createTime;
    private LocalDateTime lastMessageTime;
    private String lastMessage;
}
