package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建公告请求
 */
@Data
@Schema(description = "创建公告请求")
public class NoticeCreateRequest {

    @NotBlank(message = "标题不能为空")
    @Schema(description = "标题")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Schema(description = "内容")
    private String content;

    @Schema(description = "公告类型")
    private Integer noticeType = 1;

    @Schema(description = "紧急程度")
    private String level = "NORMAL";

    @Schema(description = "状态: 1-发布, 0-草稿")
    private Integer status = 1;
}
