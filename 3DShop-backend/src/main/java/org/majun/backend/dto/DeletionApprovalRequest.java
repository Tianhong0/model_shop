package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.majun.backend.enums.DeletionStatus;

/**
 * 注销申请审批请求DTO
 */
@Data
@Schema(description = "注销申请审批请求")
public class DeletionApprovalRequest {

    @NotNull(message = "申请ID不能为空")
    @Schema(description = "注销申请ID")
    private Long id;

    @NotNull(message = "审批状态不能为空")
    @Schema(description = "审批状态: APPROVED/REJECTED")
    private DeletionStatus status;

    @Schema(description = "管理员备注")
    private String adminNote;
}
