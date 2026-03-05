package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "邮箱验证码重置密码请求")
public class EmailResetPasswordRequest {

    @NotBlank(message = "登录账户不能为空")
    @Schema(description = "登录账户", example = "test_user")
    private String userName;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", example = "user@example.com")
    private String email;

    @NotBlank(message = "邮箱验证码不能为空")
    @Schema(description = "邮箱验证码", example = "123456")
    private String emailCode;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,20}$",
            message = "密码必须包含大小写字母和数字，长度为8-20位")
    @Schema(description = "新密码", example = "NewPass123")
    private String newPassword;

    @NotBlank(message = "确认新密码不能为空")
    @Schema(description = "确认新密码", example = "NewPass123")
    private String confirmNewPassword;
}
