package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新公告请求
 */
@Data
@Schema(description = "更新公告请求")
public class NoticeUpdateRequest {

    @NotNull(message = "ID不能为空")
    @Schema(description = "ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "公告类型")
    private Integer noticeType;

    @Schema(description = "紧急程度")
    private String level;

    @Schema(description = "状态: 1-发布, 0-草稿")
    private Integer status;
}
