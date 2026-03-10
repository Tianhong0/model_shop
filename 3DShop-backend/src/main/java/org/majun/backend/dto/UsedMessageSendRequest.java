package org.majun.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UsedMessageSendRequest {

    @NotNull(message = "商品ID不能为空")
    private Long listingId;

    @NotNull(message = "会话对方不能为空")
    private Long counterpartId;

    @NotBlank(message = "消息内容不能为空")
    private String content;

    private String attachments;

    private Integer messageType = 1;
}
