package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "悬赏任务分页查询")
public class BountyTaskQueryRequest {

    @Min(value = 1, message = "页码必须大于等于1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页大小必须大于等于1")
    private Integer pageSize = 10;

    @Schema(description = "任务状态")
    private Integer status;

    @Schema(description = "关键词(标题)")
    private String keyword;

    @Schema(description = "是否仅看我发布的任务")
    private Boolean onlyMine;
}
