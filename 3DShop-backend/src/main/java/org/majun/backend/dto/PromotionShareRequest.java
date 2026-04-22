package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "推广分享请求")
public class PromotionShareRequest {

    @NotBlank(message = "分享类型不能为空")
    @Schema(description = "分享类型：MODEL/POSTER/LINK")
    private String shareType;

    @Schema(description = "分享渠道")
    private String shareChannel;

    @Schema(description = "关联类型")
    private String refType;

    @Schema(description = "关联ID")
    private Long refId;

    @Schema(description = "分享链接")
    private String shareUrl;

    @Schema(description = "海报图片URL")
    private String posterUrl;
}
