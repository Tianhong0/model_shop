package org.majun.backend.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsConversationDetailVO {

    private CsConversationVO conversation;
    private List<CsMessageVO> messages;
}
