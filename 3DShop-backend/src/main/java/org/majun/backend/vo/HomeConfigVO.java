package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 小程序首页配置VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "小程序首页配置")
public class HomeConfigVO {

    @Schema(description = "轮播图列表")
    private List<BannerVO> banners;

    @Schema(description = "公告列表")
    private List<NoticeVO> notices;
}
