package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建帖子分类")
public class PostCategoryCreateRequest {

    @NotBlank(message = "分类名称不能为空")
    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "排序编号")
    private Integer sortNo = 0;

    @Schema(description = "状态: 1启用/0禁用")
    private Integer status = 1;
}
