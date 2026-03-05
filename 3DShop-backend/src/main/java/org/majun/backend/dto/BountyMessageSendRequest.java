package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "悬赏消息发送请求")
public class BountyMessageSendRequest {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotBlank(message = "消息内容不能为空")
    private String content;

    @Schema(description = "消息类型:1文本,2图片,3系统")
    private Integer messageType = 1;

    @Schema(description = "附件地址，多地址逗号分隔")
    private String attachments;
}
