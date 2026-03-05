package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "创建帖子请求")
public class PostCreateRequest {

    @NotNull(message = "分类不能为空")
    @Schema(description = "分类ID")
    private Long categoryId;

    @NotBlank(message = "标题不能为空")
    @Schema(description = "标题")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Schema(description = "内容")
    private String content;

    @Schema(description = "状态: 0-草稿, 1-发布")
    private Integer status = 1;

    @Valid
    @Schema(description = "媒体列表")
    private List<PostMediaItemRequest> mediaList;
}
