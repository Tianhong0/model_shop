package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "后台运营状态更新请求")
public class AdminOperationStatusUpdateRequest {

    @NotNull(message = "运营状态不能为空")
    @Schema(description = "是否营业中")
    private Boolean operating;
}
