package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 轮播图VO
 */
@Data
@Schema(description = "轮播图VO")
public class BannerVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "图片地址")
    private String imageUrl;

    @Schema(description = "跳转类型")
    private Integer linkType;

    @Schema(description = "跳转值")
    private String linkValue;

    @Schema(description = "排序")
    private Integer sortNo;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "生效开始时间")
    private LocalDateTime startTime;

    @Schema(description = "生效结束时间")
    private LocalDateTime endTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
