package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.Result;
import org.majun.backend.dto.AdminRegisterApplyRequest;
import org.majun.backend.dto.AdminRegisterReviewRequest;
import org.majun.backend.dto.EmailCodeSendRequest;
import org.majun.backend.dto.EmailResetPasswordRequest;
import org.majun.backend.dto.ForgotPasswordCodeSendRequest;
import org.majun.backend.dto.LoginRequest;
import org.majun.backend.dto.RegisterRequest;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.AuthService;
import org.majun.backend.vo.AdminRegisterRequestVO;
import org.majun.backend.vo.LoginResponse;
import org.majun.backend.vo.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Tag(name = "认证管理", description = "登录注册相关接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "根据登录账户(用户名或邮箱)和密码进行登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success("登录成功", response);
    }

    /**
     * 管理员登录
     */
    @Operation(summary = "管理员登录", description = "仅管理员角色可登录后台管理系统，支持用户名或邮箱")
    @PostMapping("/admin/login")
    public Result<LoginResponse> adminLogin(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.adminLogin(request);
        return Result.success("登录成功", response);
    }

    /**
     * 用户注册
     */
    @Operation(summary = "用户注册", description = "注册新用户")
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(request);
        return Result.success("注册成功", response);
    }

    @Operation(summary = "发送注册邮箱验证码", description = "发送用于注册的邮箱验证码")
    @PostMapping("/email-code/register")
    public Result<Void> sendRegisterEmailCode(@Valid @RequestBody EmailCodeSendRequest request) {
        authService.sendRegisterEmailCode(request.getEmail());
        return Result.success("验证码已发送，请注意查收邮箱");
    }

    @Operation(summary = "发送后台管理员注册验证码", description = "发送用于后台管理员注册申请的邮箱验证码")
    @PostMapping("/email-code/admin-register")
    public Result<Void> sendAdminRegisterEmailCode(@Valid @RequestBody EmailCodeSendRequest request) {
        authService.sendAdminRegisterEmailCode(request.getEmail());
        return Result.success("验证码已发送，请注意查收邮箱");
    }

    @Operation(summary = "提交后台管理员注册申请", description = "提交后台管理员账号注册申请，需已有管理员审核")
    @PostMapping("/admin/register-request")
    public Result<Void> submitAdminRegisterRequest(@Valid @RequestBody AdminRegisterApplyRequest request) {
        authService.submitAdminRegisterRequest(request);
        return Result.success("注册申请已提交，等待管理员审核");
    }

    @Operation(summary = "分页查询后台管理员注册申请", description = "管理员分页查询后台管理员注册申请")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/admin/register-requests")
    public Result<PageResult<AdminRegisterRequestVO>> getAdminRegisterRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success("查询成功", authService.getAdminRegisterRequests(status, pageNum, pageSize));
    }

    @Operation(summary = "审核后台管理员注册申请", description = "管理员审核后台管理员注册申请")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/admin/register-request/review")
    public Result<Void> reviewAdminRegisterRequest(@AuthenticationPrincipal LoginUser loginUser,
                                                   @Valid @RequestBody AdminRegisterReviewRequest request) {
        authService.reviewAdminRegisterRequest(loginUser.getId(), request);
        return Result.success("审核完成");
    }

    @Operation(summary = "发送忘记密码邮箱验证码", description = "根据账号和邮箱发送找回密码验证码")
    @PostMapping("/email-code/forgot-password")
    public Result<Void> sendForgotPasswordEmailCode(@Valid @RequestBody ForgotPasswordCodeSendRequest request) {
        authService.sendForgotPasswordEmailCode(request.getUserName(), request.getEmail());
        return Result.success("验证码已发送，请注意查收邮箱");
    }

    @Operation(summary = "邮箱验证码重置密码", description = "通过登录账号+邮箱+验证码重置密码")
    @PostMapping("/password/reset-by-email")
    public Result<Void> resetPasswordByEmail(@Valid @RequestBody EmailResetPasswordRequest request) {
        authService.resetPasswordByEmail(request);
        return Result.success("密码重置成功，请重新登录");
    }

    /**
     * 用户退出登录
     */
    @Operation(summary = "用户退出登录", description = "退出当前登录用户")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = authService.getTokenFromRequest(request);
        authService.logout(token);
        return Result.success("退出登录成功");
    }
}
