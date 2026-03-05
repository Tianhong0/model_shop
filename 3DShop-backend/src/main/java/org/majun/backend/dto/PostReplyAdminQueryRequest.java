package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "后台回复分页查询")
public class PostReplyAdminQueryRequest {

    @Min(value = 1, message = "页码最小为1")
    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页大小最小为1")
    @Schema(description = "每页大小")
    private Integer pageSize = 10;

    @Schema(description = "帖子ID")
    private Long postId;

    @Schema(description = "回复者ID")
    private Long userId;

    @Schema(description = "状态: 1正常/0屏蔽")
    private Integer status;

    @Schema(description = "是否采纳: 1是/0否")
    private Integer isAdopted;
}
