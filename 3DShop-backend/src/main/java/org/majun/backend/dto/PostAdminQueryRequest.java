package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "后台帖子分页查询")
public class PostAdminQueryRequest {

    @Min(value = 1, message = "页码最小为1")
    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页大小最小为1")
    @Schema(description = "每页大小")
    private Integer pageSize = 10;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "发布者ID")
    private Long userId;

    @Schema(description = "状态: 0草稿/1发布/2下架")
    private Integer status;

    @Schema(description = "是否置顶: 0否/1是")
    private Integer isTop;

    @Schema(description = "关键词(标题/内容)")
    private String keyword;
}
