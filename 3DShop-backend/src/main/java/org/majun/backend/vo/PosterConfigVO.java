package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推广海报配置VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "推广海报配置")
public class PosterConfigVO {

    @Schema(description = "海报标题")
    private String title;

    @Schema(description = "海报副标题")
    private String subtitle;

    @Schema(description = "背景渐变起始色")
    private String bgColorStart;

    @Schema(description = "背景渐变结束色")
    private String bgColorEnd;

    @Schema(description = "背景图片URL")
    private String bgImage;

    @Schema(description = "标题文字颜色")
    private String titleColor;

    @Schema(description = "邀请码文字颜色")
    private String codeColor;

    @Schema(description = "底部提示文字")
    private String tipsText;

    @Schema(description = "海报宽度")
    private Integer width;

    @Schema(description = "海报高度")
    private Integer height;

    @Schema(description = "二维码尺寸")
    private Integer qrcodeSize;

    @Schema(description = "邀请注册奖励积分")
    private Integer inviteRegisterPoints;

    @Schema(description = "首单奖励积分")
    private Integer firstOrderPoints;

    @Schema(description = "消费返积分比例")
    private String consumeRebateRate;
}
