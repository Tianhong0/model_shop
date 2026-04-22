package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 注册请求DTO
 */
@Data
@Schema(description = "注册请求")
public class RegisterRequest {

    /**
     * 登录账户
     */
    @NotBlank(message = "登录账户不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "登录账户由4-20位字母、数字或下划线组成")
    @Schema(description = "登录账户", example = "test_user")
    private String userName;

    /**
     * 账户密码
     */
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,20}$",
            message = "密码必须包含大小写字母和数字，长度为8-20位")
    @Schema(description = "账户密码", example = "Test1234")
    private String password;

    /**
     * 确认密码
     */
    @NotBlank(message = "确认密码不能为空")
    @Schema(description = "确认密码", example = "Test1234")
    private String confirmPassword;

    /**
     * 显示名称/昵称
     */
    @NotBlank(message = "昵称不能为空")
    @Schema(description = "显示名称/昵称", example = "测试用户")
    private String nickname;

    /**
     * 电话
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "电话", example = "13800138000")
    private String mobile;

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", example = "user@example.com")
    private String email;

    /**
     * 邮箱验证码
     */
    @NotBlank(message = "邮箱验证码不能为空")
    @Schema(description = "邮箱验证码", example = "123456")
    private String emailCode;

    /**
     * 用户头像URL
     */
    @Schema(description = "用户头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    /**
     * 邀请码
     */
    @Schema(description = "邀请码", example = "ABC123")
    private String inviteCode;
}
