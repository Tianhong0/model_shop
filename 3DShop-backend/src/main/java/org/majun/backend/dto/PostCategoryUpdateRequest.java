package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "更新帖子分类")
public class PostCategoryUpdateRequest {

    @NotNull(message = "分类ID不能为空")
    @Schema(description = "分类ID")
    private Long id;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "排序编号")
    private Integer sortNo;

    @Schema(description = "状态: 1启用/0禁用")
    private Integer status;
}
