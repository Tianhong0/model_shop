package org.majun.backend.service;

import jakarta.servlet.http.HttpServletRequest;
import org.majun.backend.dto.AdminRegisterApplyRequest;
import org.majun.backend.dto.AdminRegisterReviewRequest;
import org.majun.backend.dto.EmailResetPasswordRequest;
import org.majun.backend.dto.LoginRequest;
import org.majun.backend.dto.RegisterRequest;
import org.majun.backend.vo.AdminRegisterRequestVO;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.LoginResponse;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应
     */
    LoginResponse login(LoginRequest request);

    /**
     * 管理员登录
     *
     * @param request 登录请求
     * @return 登录响应
     */
    LoginResponse adminLogin(LoginRequest request);

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 登录响应
     */
    LoginResponse register(RegisterRequest request);

    /**
     * 发送注册邮箱验证码
     */
    void sendRegisterEmailCode(String email);

    /**
     * 发送后台管理员注册申请邮箱验证码
     */
    void sendAdminRegisterEmailCode(String email);

    /**
     * 提交后台管理员注册申请
     */
    void submitAdminRegisterRequest(AdminRegisterApplyRequest request);

    /**
     * 分页查询后台管理员注册申请
     */
    PageResult<AdminRegisterRequestVO> getAdminRegisterRequests(String status, Integer pageNum, Integer pageSize);

    /**
     * 审核后台管理员注册申请
     */
    void reviewAdminRegisterRequest(Long reviewerId, AdminRegisterReviewRequest request);

    /**
     * 发送忘记密码邮箱验证码
     */
    void sendForgotPasswordEmailCode(String userName, String email);

    /**
     * 通过邮箱验证码重置密码
     */
    void resetPasswordByEmail(EmailResetPasswordRequest request);

    /**
     * 从请求中获取 Token
     */
    String getTokenFromRequest(HttpServletRequest request);

    /**
     * 用户退出登录
     */
    void logout(String token);
}
