package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.common.ResultCode;
import org.majun.backend.config.JwtProperties;
import org.majun.backend.dto.AdminRegisterApplyRequest;
import org.majun.backend.dto.AdminRegisterReviewRequest;
import org.majun.backend.dto.EmailResetPasswordRequest;
import org.majun.backend.dto.LoginRequest;
import org.majun.backend.dto.RegisterRequest;
import org.majun.backend.entity.AdminRegisterRequest;
import org.majun.backend.enums.AdminRegisterStatus;
import org.majun.backend.enums.EmailCodeScene;
import org.majun.backend.entity.SysRole;
import org.majun.backend.entity.SysUser;
import org.majun.backend.entity.SysUserRole;
import org.majun.backend.repository.AdminRegisterRequestRepository;
import org.majun.backend.repository.SysRoleRepository;
import org.majun.backend.repository.SysUserRepository;
import org.majun.backend.repository.SysUserRoleRepository;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.AuthService;
import org.majun.backend.service.EmailCodeService;
import org.majun.backend.util.JwtUtil;
import org.majun.backend.util.RedisUtil;
import org.majun.backend.vo.AdminRegisterRequestVO;
import org.majun.backend.vo.LoginResponse;
import org.majun.backend.vo.PageResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserRepository userRepository;
    private final AdminRegisterRequestRepository adminRegisterRequestRepository;
    private final SysRoleRepository roleRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final RedisUtil redisUtil;
    private final UserDetailsService userDetailsService;
    private final EmailCodeService emailCodeService;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Override
    public LoginResponse login(LoginRequest request) {
        return doLogin(request, false);
    }

    @Override
    public LoginResponse adminLogin(LoginRequest request) {
        return doLogin(request, true);
    }

    private LoginResponse doLogin(LoginRequest request, boolean adminOnly) {
        String loginUserName = resolveLoginUserName(request.getUserName());

        // 1. 使用 AuthenticationManager 认证用户
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginUserName, request.getPassword())
        );

        // 2. 获取认证后的用户信息
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        SysUser user = loginUser.getUser();

        // 3. 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUserName());

        // 3.5. 将 Token 存入 Redis，设置过期时间
        redisUtil.setToken(token, user.getId());

        List<String> roles = loginUser.getAuthorities().stream()
            .map(auth -> auth.getAuthority())
            .collect(Collectors.toList());

        if (adminOnly && roles.stream().noneMatch("ROLE_ADMIN"::equals)) {
            redisUtil.deleteToken(token);
            throw new BusinessException(ResultCode.PERMISSION_DENIED);
        }

        // 4. 构建返回结果
        return LoginResponse.builder()
                .userId(user.getId())
                .userName(user.getUserName())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .token(token)
                .tokenExpireTime(System.currentTimeMillis() + jwtProperties.getExpiration())
            .roles(roles)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // 1. 校验两次密码是否一致
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }

        emailCodeService.verifyCode(EmailCodeScene.REGISTER, normalizedEmail, request.getEmailCode(), true);

        // 2. 检查用户名是否已存在
        SysUser existUser = userRepository.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUserName, request.getUserName())
        );
        if (existUser != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        SysUser existEmailUser = userRepository.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getEmail, normalizedEmail)
        );
        if (existEmailUser != null) {
            throw new BusinessException("该邮箱已被注册");
        }

        // 3. 检查手机号是否已存在
        if (request.getMobile() != null && !request.getMobile().isEmpty()) {
            SysUser existMobileUser = userRepository.selectOne(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getMobile, request.getMobile())
            );
            if (existMobileUser != null) {
                throw new BusinessException("该手机号已被注册");
            }
        }

        // 4. 创建用户
        SysUser user = new SysUser();
        user.setUserName(request.getUserName());
        user.setUserPwd(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setMobile(request.getMobile());
        user.setEmail(normalizedEmail);
        user.setAvatar(request.getAvatar()); // 设置用户头像
        user.setStatus(1); // 正常状态
        user.setIsDelete(0); // 未删除

        int insertResult = userRepository.insert(user);
        if (insertResult <= 0) {
            throw new BusinessException("注册失败");
        }

        // 5. 分配默认角色（普通用户角色）
        // 查询普通用户角色
        SysRole userRole = roleRepository.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleName, "ROLE_USER")
                );

        if (userRole != null) {
            // 创建用户角色关联
            SysUserRole userRoleRelation = new SysUserRole();
            userRoleRelation.setUserId(user.getId());
            userRoleRelation.setRoleId(userRole.getId());
            userRoleRepository.insert(userRoleRelation);
        } else {
            log.warn("未找到 ROLE_USER 角色，用户 {} 未分配角色", request.getUserName());
        }

        // 6. 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUserName());

        // 6.5. 将 Token 存入 Redis，设置过期时间
        redisUtil.setToken(token, user.getId());

        // 7. 构建返回结果
        return LoginResponse.builder()
                .userId(user.getId())
                .userName(user.getUserName())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .token(token)
                .tokenExpireTime(System.currentTimeMillis() + jwtProperties.getExpiration())
                .roles(List.of("ROLE_USER"))
                .build();
    }

    @Override
    public void sendRegisterEmailCode(String email) {
        SysUser existEmailUser = userRepository.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getEmail, email.trim().toLowerCase())
        );
        if (existEmailUser != null) {
            throw new BusinessException("该邮箱已被注册");
        }
        emailCodeService.sendCode(EmailCodeScene.REGISTER, email);
    }

    @Override
    public void sendAdminRegisterEmailCode(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        SysUser existEmailUser = userRepository.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getEmail, normalizedEmail)
        );
        if (existEmailUser != null) {
            throw new BusinessException("该邮箱已被使用");
        }
        emailCodeService.sendCode(EmailCodeScene.ADMIN_REGISTER, normalizedEmail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitAdminRegisterRequest(AdminRegisterApplyRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }

        String userName = request.getUserName().trim();
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        emailCodeService.verifyCode(EmailCodeScene.ADMIN_REGISTER, normalizedEmail, request.getEmailCode(), true);

        SysUser existUser = userRepository.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUserName, userName)
        );
        if (existUser != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        SysUser existEmailUser = userRepository.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getEmail, normalizedEmail)
        );
        if (existEmailUser != null) {
            throw new BusinessException("该邮箱已被使用");
        }

        AdminRegisterRequest pendingRequest = adminRegisterRequestRepository.findPendingByUserNameOrEmail(userName, normalizedEmail);
        if (pendingRequest != null) {
            throw new BusinessException("已有待审核申请，请耐心等待管理员处理");
        }

        AdminRegisterRequest latestRequest = adminRegisterRequestRepository.findLatestByUserNameOrEmail(userName, normalizedEmail);
        if (latestRequest != null
                && AdminRegisterStatus.REJECTED.getCode().equals(latestRequest.getStatus())
                && latestRequest.getRetryAfter() != null
                && latestRequest.getRetryAfter().isAfter(LocalDateTime.now())) {
            throw new BusinessException("申请被驳回后24小时内不可重复提交");
        }

        AdminRegisterRequest newRequest = new AdminRegisterRequest();
        newRequest.setUserName(userName);
        newRequest.setNickname(request.getNickname().trim());
        newRequest.setMobile(request.getMobile());
        newRequest.setEmail(normalizedEmail);
        newRequest.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newRequest.setStatus(AdminRegisterStatus.PENDING.getCode());
        newRequest.setRequestTime(LocalDateTime.now());
        adminRegisterRequestRepository.insert(newRequest);
    }

    @Override
    public PageResult<AdminRegisterRequestVO> getAdminRegisterRequests(String status, Integer pageNum, Integer pageSize) {
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;

        LambdaQueryWrapper<AdminRegisterRequest> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(AdminRegisterRequest::getStatus, status.trim().toLowerCase());
        }
        queryWrapper.orderByDesc(AdminRegisterRequest::getRequestTime);

        Page<AdminRegisterRequest> page = new Page<>(currentPage, currentSize);
        adminRegisterRequestRepository.selectPage(page, queryWrapper);

        List<AdminRegisterRequestVO> records = page.getRecords().stream().map(this::convertAdminRegisterRequestVO).toList();
        return PageResult.<AdminRegisterRequestVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewAdminRegisterRequest(Long reviewerId, AdminRegisterReviewRequest request) {
        AdminRegisterRequest apply = adminRegisterRequestRepository.selectById(request.getId());
        if (apply == null) {
            throw new BusinessException("管理员注册申请不存在");
        }
        if (!AdminRegisterStatus.PENDING.getCode().equals(apply.getStatus())) {
            throw new BusinessException("该申请已处理，请勿重复审核");
        }

        AdminRegisterStatus status = request.getStatus();
        if (status != AdminRegisterStatus.APPROVED && status != AdminRegisterStatus.REJECTED) {
            throw new BusinessException("无效的审核状态");
        }
        apply.setStatus(status.getCode());
        apply.setReviewBy(reviewerId);
        apply.setReviewTime(LocalDateTime.now());
        apply.setReviewRemark(request.getReviewRemark());

        if (status == AdminRegisterStatus.APPROVED) {
            SysUser existUser = userRepository.selectOne(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getUserName, apply.getUserName())
            );
            if (existUser != null) {
                throw new BusinessException("该登录账户已存在，无法通过审核");
            }

            SysUser existEmail = userRepository.selectOne(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getEmail, apply.getEmail())
            );
            if (existEmail != null) {
                throw new BusinessException("该邮箱已被使用，无法通过审核");
            }

            SysRole adminRole = roleRepository.selectOne(
                    new LambdaQueryWrapper<SysRole>()
                            .eq(SysRole::getRoleName, "ROLE_ADMIN")
            );
            if (adminRole == null) {
                throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
            }

            SysUser user = new SysUser();
            user.setUserName(apply.getUserName());
            user.setNickname(apply.getNickname());
            user.setMobile(apply.getMobile());
            user.setEmail(apply.getEmail());
            user.setUserPwd(apply.getPasswordHash());
            user.setStatus(1);
            user.setIsDelete(0);
            userRepository.insert(user);

            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(adminRole.getId());
            userRoleRepository.insert(userRole);

            apply.setApprovedUserId(user.getId());
            apply.setRetryAfter(null);
            sendAdminRegisterResultEmail(apply.getEmail(), apply.getUserName(), true, request.getReviewRemark());
        } else if (status == AdminRegisterStatus.REJECTED) {
            apply.setRetryAfter(LocalDateTime.now().plusHours(24));
            sendAdminRegisterResultEmail(apply.getEmail(), apply.getUserName(), false, request.getReviewRemark());
        }

        adminRegisterRequestRepository.updateById(apply);
    }

    @Override
    public void sendForgotPasswordEmailCode(String userName, String email) {
        String loginAccount = userName == null ? "" : userName.trim();
        String normalizedEmail = email.trim().toLowerCase();
        SysUser user = findUserByLoginAccountAndEmail(loginAccount, normalizedEmail);
        if (user == null) {
            throw new BusinessException("账号与邮箱不匹配");
        }
        emailCodeService.sendCode(EmailCodeScene.FORGOT_PASSWORD, normalizedEmail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPasswordByEmail(EmailResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String loginAccount = request.getUserName() == null ? "" : request.getUserName().trim();
        SysUser user = findUserByLoginAccountAndEmail(loginAccount, normalizedEmail);
        if (user == null) {
            throw new BusinessException("账号与邮箱不匹配");
        }

        emailCodeService.verifyCode(EmailCodeScene.FORGOT_PASSWORD, normalizedEmail, request.getEmailCode(), true);

        user.setUserPwd(passwordEncoder.encode(request.getNewPassword()));
        int updated = userRepository.updateById(user);
        if (updated <= 0) {
            throw new BusinessException("重置密码失败");
        }

        redisUtil.deleteTokenByUserId(user.getId());
        redisUtil.deleteLoginUser("user:details:" + user.getUserName());
    }

    // 删除旧的logout方法，因为现在在Controller层处理token获取

    /**
     * 从请求中获取 Token
     */
    @Override
    public String getTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(jwtProperties.getHeader());
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(jwtProperties.getPrefix())) {
            return authHeader.substring(jwtProperties.getPrefix().length());
        }
        return null;
    }

    @Override
    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            // 获取用户名，用于清除用户详情缓存
            try {
                String username = jwtUtil.getUserName(token);
                if (username != null) {
                    redisUtil.deleteLoginUser("user:details:" + username);
                }
            } catch (Exception e) {
                log.warn("解析token获取用户名失败: {}", e.getMessage());
            }
            // 将 Token 从 Redis 中删除
            redisUtil.deleteToken(token);
            log.info("用户退出登录成功，token及缓存已删除");
        } else {
            log.warn("退出登录失败，未找到有效的token");
        }
    }

    private AdminRegisterRequestVO convertAdminRegisterRequestVO(AdminRegisterRequest request) {
        AdminRegisterRequestVO vo = new AdminRegisterRequestVO();
        vo.setId(request.getId());
        vo.setUserName(request.getUserName());
        vo.setNickname(request.getNickname());
        vo.setMobile(request.getMobile());
        vo.setEmail(request.getEmail());
        vo.setStatus(request.getStatus());
        vo.setRequestTime(request.getRequestTime());
        vo.setReviewTime(request.getReviewTime());
        vo.setReviewBy(request.getReviewBy());
        vo.setReviewRemark(request.getReviewRemark());
        vo.setRetryAfter(request.getRetryAfter());
        vo.setApprovedUserId(request.getApprovedUserId());
        return vo;
    }

    private String resolveLoginUserName(String loginAccount) {
        if (!StringUtils.hasText(loginAccount)) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        String account = loginAccount.trim();
        if (!account.contains("@")) {
            return account;
        }

        SysUser user = userRepository.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, account.toLowerCase()));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user.getUserName();
    }

    private SysUser findUserByLoginAccountAndEmail(String loginAccount, String normalizedEmail) {
        String normalizedAccount = StringUtils.hasText(loginAccount) ? loginAccount.trim().toLowerCase() : "";
        return userRepository.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, normalizedEmail)
                .and(wrapper -> wrapper.eq(SysUser::getUserName, loginAccount)
                        .or()
                        .eq(SysUser::getEmail, normalizedAccount)));
    }

    private void sendAdminRegisterResultEmail(String email, String userName, boolean approved, String reviewRemark) {
        if (!StringUtils.hasText(mailFrom) || !StringUtils.hasText(email)) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(email);
            message.setSubject(approved ? "后台管理员注册申请审核通过" : "后台管理员注册申请审核结果通知");
            String resultText = approved ? "已通过" : "未通过";
            String remarkText = StringUtils.hasText(reviewRemark) ? reviewRemark : "无";
            message.setText("您好，" + userName + "\n\n您的后台管理员注册申请审核结果：" + resultText
                    + "。\n审核备注：" + remarkText
                    + (approved ? "\n\n您现在可以使用该账号登录后台管理系统。" : "\n\n您可在24小时后重新发起申请。")
                    + "\n\n3DShop 管理系统");
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("发送管理员注册审核结果邮件失败, email={}", email, e);
        }
    }
}
