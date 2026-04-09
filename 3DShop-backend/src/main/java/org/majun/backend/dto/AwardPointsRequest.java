package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 颁奖积分请求
 */
@Data
@Schema(description = "颁奖积分请求")
public class AwardPointsRequest {

    @NotNull(message = "活动ID不能为空")
    @Schema(description = "活动ID")
    private Long eventId;

    @NotEmpty(message = "参与记录ID列表不能为空")
    @Schema(description = "参与记录ID列表")
    private List<Long> participationIds;
}
