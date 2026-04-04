package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 聊天响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI聊天响应")
public class AiChatVO {

    @Schema(description = "消息ID")
    private Long messageId;

    @Schema(description = "AI回复内容")
    private String content;

    @Schema(description = "意图类型")
    private String intent;

    @Schema(description = "置信度")
    private Double confidence;

    @Schema(description = "AI提供商")
    private String provider;

    @Schema(description = "消耗Token数")
    private Integer tokensUsed;

    @Schema(description = "是否转人工")
    private Boolean transferToHuman;

    @Schema(description = "会话摘要")
    private String summary;

    @Schema(description = "是否成功")
    private Boolean success;

    @Schema(description = "错误信息")
    private String errorMessage;
}
