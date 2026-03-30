package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;

/**
 * 创建悬赏评价请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "创建悬赏评价请求")
public class BountyRatingCreateRequest {

    @Schema(description = "任务ID")
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @Schema(description = "评分1-5星")
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    private Integer score;

    @Schema(description = "评价内容")
    @Size(max = 1000, message = "评价内容最多1000字")
    private String comment;

    @Schema(description = "评价图片，逗号分隔")
    private String images;

    @Schema(description = "是否匿名: 1是, 0否")
    private Integer isAnonymous;
}
