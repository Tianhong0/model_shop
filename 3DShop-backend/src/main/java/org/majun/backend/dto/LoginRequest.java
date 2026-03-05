package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求DTO
 */
@Data
@Schema(description = "登录请求")
public class LoginRequest {

    /**
     * 登录账户
     */
    @NotBlank(message = "登录账户不能为空")
    @Schema(description = "登录账户(用户名或邮箱)", example = "admin@example.com")
    private String userName;

    /**
     * 账户密码
     */
    @NotBlank(message = "密码不能为空")
    @Schema(description = "账户密码", example = "123456")
    private String password;
}
