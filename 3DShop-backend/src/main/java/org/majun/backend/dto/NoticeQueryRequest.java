package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 公告查询请求
 */
@Data
@Schema(description = "公告查询请求")
public class NoticeQueryRequest {

    @Min(value = 1, message = "页码最小为1")
    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页大小最小为1")
    @Schema(description = "每页大小")
    private Integer pageSize = 10;

    @Schema(description = "标题关键词")
    private String title;

    @Schema(description = "公告类型")
    private Integer noticeType;

    @Schema(description = "状态")
    private Integer status;
}
