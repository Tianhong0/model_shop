package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建轮播图请求
 */
@Data
@Schema(description = "创建轮播图请求")
public class BannerCreateRequest {

    @NotBlank(message = "标题不能为空")
    @Schema(description = "标题")
    private String title;

    @Schema(description = "副标题")
    private String subtitle;

    @NotBlank(message = "图片地址不能为空")
    @Schema(description = "图片地址")
    private String imageUrl;

    @Schema(description = "跳转类型")
    private Integer linkType = 0;

    @Schema(description = "跳转值")
    private String linkValue;

    @Schema(description = "排序值")
    private Integer sortNo = 0;

    @Schema(description = "状态: 1-启用, 0-禁用")
    private Integer status = 1;

    @Schema(description = "生效开始时间")
    private LocalDateTime startTime;

    @Schema(description = "生效结束时间")
    private LocalDateTime endTime;
}
