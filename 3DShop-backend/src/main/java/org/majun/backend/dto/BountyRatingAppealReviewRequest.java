package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;

/**
 * 管理员审核申诉请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理员审核申诉请求")
public class BountyRatingAppealReviewRequest {

    @Schema(description = "申诉ID")
    @NotNull(message = "申诉ID不能为空")
    private Long appealId;

    @Schema(description = "审核结论: 1通过, 2驳回")
    @NotNull(message = "审核结论不能为空")
    private Integer decision;

    @Schema(description = "管理员备注")
    @Size(max = 500, message = "备注最多500字")
    private String adminRemark;
}
