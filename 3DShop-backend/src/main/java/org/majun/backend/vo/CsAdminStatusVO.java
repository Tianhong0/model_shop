package org.majun.backend.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsAdminStatusVO {

    private Long adminId;
    private String adminNickname;
    private Boolean isOnline;
    private Integer currentConversationCount;
    private Integer totalServedCount;
}
