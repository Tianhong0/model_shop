package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "我的交互帖子查询")
public class PostInteractionMyQueryRequest {

    @Min(value = 1, message = "页码最小为1")
    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页大小最小为1")
    @Schema(description = "每页大小")
    private Integer pageSize = 10;

    @NotNull(message = "交互类型不能为空")
    @Schema(description = "交互类型: 1-点赞, 2-收藏")
    private Integer interactType;
}
