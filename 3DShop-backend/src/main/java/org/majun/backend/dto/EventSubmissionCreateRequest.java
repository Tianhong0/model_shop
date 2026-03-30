package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 作品提交请求
 */
@Data
@Schema(description = "作品提交请求")
public class EventSubmissionCreateRequest {

    @NotNull(message = "活动ID不能为空")
    @Schema(description = "活动ID")
    private Long eventId;

    @NotBlank(message = "作品标题不能为空")
    @Schema(description = "作品标题")
    private String title;

    @Schema(description = "作品描述")
    private String description;

    @Schema(description = "作品文件URL列表")
    private List<String> fileUrls;

    @Schema(description = "作品图片URL列表")
    private List<String> imageUrls;
}
