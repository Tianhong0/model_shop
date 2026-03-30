package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;

/**
 * 创建评价申诉请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "创建评价申诉请求")
public class BountyRatingAppealCreateRequest {

    @Schema(description = "评价ID")
    @NotNull(message = "评价ID不能为空")
    private Long ratingId;

    @Schema(description = "申诉理由")
    @NotBlank(message = "申诉理由不能为空")
    @Size(max = 1000, message = "申诉理由最多1000字")
    private String reason;

    @Schema(description = "证据材料URL，逗号分隔")
    private String evidence;
}
