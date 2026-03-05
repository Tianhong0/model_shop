package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户注销申请请求DTO
 */
@Data
@Schema(description = "用户注销申请请求")
public class DeletionRequestDTO {

    @NotBlank(message = "注销原因不能为空")
    @Schema(description = "注销原因", example = "不再使用该平台")
    private String reason;
}
