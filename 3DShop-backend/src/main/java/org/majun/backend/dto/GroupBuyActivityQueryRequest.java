package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 拼团活动查询请求
 */
@Data
@Schema(description = "拼团活动查询请求")
public class GroupBuyActivityQueryRequest {

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页数量")
    private Integer pageSize = 10;

    @Schema(description = "活动名称")
    private String activityName;

    @Schema(description = "模型ID")
    private Long modelId;

    @Schema(description = "状态")
    private Integer status;
}
