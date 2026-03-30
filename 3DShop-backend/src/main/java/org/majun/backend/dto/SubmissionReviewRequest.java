package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 作品审核请求
 */
@Data
@Schema(description = "作品审核请求")
public class SubmissionReviewRequest {

    @NotNull(message = "作品ID不能为空")
    @Schema(description = "作品ID")
    private Long submissionId;

    @NotNull(message = "审核状态不能为空")
    @Schema(description = "审核状态: 2-通过, 3-拒绝")
    private Integer status;

    @Schema(description = "审核备注")
    private String reviewRemark;

    @Schema(description = "评分(可选)")
    private java.math.BigDecimal score;
}
