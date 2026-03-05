package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.majun.backend.enums.AdminRegisterStatus;

@Data
@Schema(description = "后台管理员注册申请审核请求")
public class AdminRegisterReviewRequest {

    @NotNull(message = "申请ID不能为空")
    private Long id;

    @NotNull(message = "审核结果不能为空")
    private AdminRegisterStatus status;

    @Schema(description = "审核备注")
    private String reviewRemark;
}
