package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录响应")
public class LoginResponse {

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 登录账户
     */
    @Schema(description = "登录账户")
    private String userName;

    /**
     * 显示名称/昵称
     */
    @Schema(description = "显示名称/昵称")
    private String nickname;

    /**
     * 用户头像
     */
    @Schema(description = "用户头像")
    private String avatar;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱")
    private String email;

    /**
     * Token
     */
    @Schema(description = "Token")
    private String token;

    /**
     * Token过期时间（时间戳）
     */
    @Schema(description = "Token过期时间（时间戳）")
    private Long tokenExpireTime;

    /**
     * 角色编码列表
     */
    @Schema(description = "角色编码列表")
    private List<String> roles;
}
