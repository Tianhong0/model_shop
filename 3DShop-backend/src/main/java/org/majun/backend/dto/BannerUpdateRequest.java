package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 更新轮播图请求
 */
@Data
@Schema(description = "更新轮播图请求")
public class BannerUpdateRequest {

    @NotNull(message = "ID不能为空")
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

    @Schema(description = "排序值")
    private Integer sortNo;

    @Schema(description = "状态: 1-启用, 0-禁用")
    private Integer status;

    @Schema(description = "生效开始时间")
    private LocalDateTime startTime;

    @Schema(description = "生效结束时间")
    private LocalDateTime endTime;
}
