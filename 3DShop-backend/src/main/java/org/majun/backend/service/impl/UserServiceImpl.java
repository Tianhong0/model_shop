package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.common.ResultCode;
import org.majun.backend.dto.ChangePasswordRequest;
import org.majun.backend.dto.DeletionApprovalRequest;
import org.majun.backend.dto.DesignerApplyReviewRequest;
import org.majun.backend.dto.DesignerApplySubmitRequest;
import org.majun.backend.dto.UserQueryRequest;
import org.majun.backend.dto.UserUpdateRequest;
import org.majun.backend.dto.BatchReviewRequest;
import org.majun.backend.entity.DesignerApplyRequest;
import org.majun.backend.entity.SysRole;
import org.majun.backend.entity.SysUser;
import org.majun.backend.entity.SysUserRole;
import org.majun.backend.entity.SysModel;
import org.majun.backend.entity.UserDeletionRequest;
import org.majun.backend.enums.DesignerApplyStatus;
import org.majun.backend.enums.EmailCodeScene;
import org.majun.backend.enums.DeletionStatus;
import org.majun.backend.repository.DesignerApplyRequestRepository;
import org.majun.backend.repository.SysRoleRepository;
import org.majun.backend.repository.SysModelRepository;
import org.majun.backend.repository.SysUserRepository;
import org.majun.backend.repository.SysUserRoleRepository;
import org.majun.backend.repository.UserDeletionRequestRepository;
import org.majun.backend.service.EmailCodeService;
import org.majun.backend.service.UserService;
import org.majun.backend.vo.AdminStatsVO;
import org.majun.backend.vo.BatchOperationResultVO;
import org.majun.backend.vo.DeletionRequestVO;
import org.majun.backend.vo.DesignerApplyRequestVO;
import org.majun.backend.vo.DesignerVO;
import org.majun.backend.vo.MyDesignerApplyStatusVO;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.UserListVO;
import org.majun.backend.util.RedisUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Service
@RequiredArgsConstructor
/**
 * 用户服务实现
 */
public class UserServiceImpl implements UserService {

    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysModelRepository sysModelRepository;
    private final UserDeletionRequestRepository deletionRequestRepository;
    private final DesignerApplyRequestRepository designerApplyRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisUtil redisUtil;
    private final EmailCodeService emailCodeService;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Override
    public PageResult<UserListVO> getUserList(UserQueryRequest request) {
        int pageNum = request.getPage() == null || request.getPage() < 1 ? 1 : request.getPage();
        int pageSize = request.getSize() == null || request.getSize() < 1 ? 10 : request.getSize();

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .orderByDesc(SysUser::getCreateTime);
        if (request.getUserName() != null && !request.getUserName().isBlank()) {
            wrapper.like(SysUser::getUserName, request.getUserName().trim());
        }
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            wrapper.like(SysUser::getNickname, request.getNickname().trim());
        }
        if (request.getMobile() != null && !request.getMobile().isBlank()) {
            wrapper.like(SysUser::getMobile, request.getMobile().trim());
        }
        if (request.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, request.getStatus());
        }
        if (request.getSex() != null) {
            wrapper.eq(SysUser::getSex, request.getSex());
        }

        // 按角色筛选
        List<Long> roleUserIds = null;
        if (request.getRoleId() != null || Boolean.TRUE.equals(request.getIsAdmin())) {
            Long targetRoleId = request.getRoleId();
            if (Boolean.TRUE.equals(request.getIsAdmin())) {
                // 快捷筛选管理员
                SysRole adminRole = roleRepository.selectOne(
                        new LambdaQueryWrapper<SysRole>()
                                .eq(SysRole::getRoleName, "ROLE_ADMIN")
                                .eq(SysRole::getStatus, 1));
                if (adminRole == null) {
                    return PageResult.<UserListVO>builder()
                            .records(Collections.emptyList())
                            .total(0L)
                            .pageNum(pageNum)
                            .pageSize(pageSize)
                            .pages(0)
                            .build();
                }
                targetRoleId = adminRole.getId();
            }

            List<SysUserRole> userRoles = userRoleRepository.selectList(
                    new LambdaQueryWrapper<SysUserRole>()
                            .eq(SysUserRole::getRoleId, targetRoleId));
            roleUserIds = userRoles.stream()
                    .map(SysUserRole::getUserId)
                    .collect(Collectors.toList());

            if (roleUserIds.isEmpty()) {
                return PageResult.<UserListVO>builder()
                        .records(Collections.emptyList())
                        .total(0L)
                        .pageNum(pageNum)
                        .pageSize(pageSize)
                        .pages(0)
                        .build();
            }
            wrapper.in(SysUser::getId, roleUserIds);
        }

        Page<SysUser> page = new Page<>(pageNum, pageSize);
        userRepository.selectPage(page, wrapper);
        List<UserListVO> records = page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList());

        // 填充角色信息
        for (UserListVO vo : records) {
            List<SysUserRole> userRoles = userRoleRepository.selectList(
                    new LambdaQueryWrapper<SysUserRole>()
                            .eq(SysUserRole::getUserId, vo.getId()));

            if (!userRoles.isEmpty()) {
                List<Long> roleIds = userRoles.stream()
                        .map(SysUserRole::getRoleId)
                        .collect(Collectors.toList());
                vo.setRoleIds(roleIds);

                List<SysRole> roles = roleRepository.selectBatchIds(roleIds);
                List<String> roleNames = roles.stream()
                        .map(SysRole::getRoleName)
                        .collect(Collectors.toList());
                vo.setRoleNames(roleNames);
                vo.setIsAdmin(roleNames.contains("ROLE_ADMIN"));
            }
        }

        return PageResult.<UserListVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    public UserListVO getUserDetail(Long userId) {
        // 查询用户信息
        SysUser user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 查询用户角色
        List<SysUserRole> userRoles = userRoleRepository.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)
        );

        // 转换为VO
        UserListVO vo = convertToVO(user);
        vo.setRoleIds(userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList()));

        return vo;
    }

    @Override
    public List<DesignerVO> getDesignerList() {
        // 查询设计者角色ID
        Long designerRoleId = roleRepository.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleName, "ROLE_DESIGNER")
        ).getId();

        // 查询所有设计者
        List<SysUserRole> designerRelations = userRoleRepository.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getRoleId, designerRoleId)
        );

        List<Long> designerIds = designerRelations.stream()
                .map(SysUserRole::getUserId)
                .collect(Collectors.toList());

        // 查询设计者信息
        if (designerIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysUser> designers = userRepository.selectBatchIds(designerIds);

        return designers.stream().map(designer -> {
            DesignerVO vo = new DesignerVO();
            vo.setId(designer.getId());
            vo.setUserName(designer.getUserName());
            vo.setNickname(designer.getNickname());
            vo.setAvatar(designer.getAvatar());

            // 查询该设计者制作的模型（仅未删除的）
            List<Long> modelIds = sysModelRepository.selectList(
                    new LambdaQueryWrapper<SysModel>()
                            .eq(SysModel::getDesignerId, designer.getId())
                            .eq(SysModel::getIsDelete, 0)
                            .select(SysModel::getId)
            ).stream()
                    .map(SysModel::getId)
                    .collect(Collectors.toList());

            vo.setModelIds(modelIds);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean updateUser(UserUpdateRequest request) {
        // 查询用户信息
        SysUser user = userRepository.selectById(request.getId());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 更新用户信息（仅更新非空字段）
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getSex() != null) {
            user.setSex(request.getSex());
        }
        if (request.getMobile() != null) {
            // 检查手机号是否被其他人使用
            SysUser existMobileUser = userRepository.selectOne(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getMobile, request.getMobile())
                            .ne(SysUser::getId, request.getId())
            );
            if (existMobileUser != null) {
                throw new BusinessException("该手机号已被其他用户使用");
            }
            user.setMobile(request.getMobile());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getEmail() != null) {
            String normalizedEmail = request.getEmail().trim().toLowerCase();
            String currentEmail = user.getEmail() == null ? "" : user.getEmail().trim().toLowerCase();
            if (!normalizedEmail.equals(currentEmail)) {
                if (!StringUtils.hasText(request.getEmailCode())) {
                    throw new BusinessException("修改邮箱需要输入验证码");
                }
                SysUser existEmailUser = userRepository.selectOne(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getEmail, normalizedEmail)
                                .ne(SysUser::getId, request.getId())
                );
                if (existEmailUser != null) {
                    throw new BusinessException("该邮箱已被其他用户使用");
                }
                emailCodeService.verifyCode(EmailCodeScene.CHANGE_EMAIL, normalizedEmail, request.getEmailCode(), true);
                user.setEmail(normalizedEmail);
            }
        }

        // 保存更新
        return userRepository.updateById(user) > 0;
    }

    @Override
    public boolean changePassword(Long userId, ChangePasswordRequest request) {
        // 校验两次新密码是否一致
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }

        // 查询用户信息
        SysUser user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new BusinessException("当前账号未绑定邮箱，请先绑定邮箱");
        }

        // 校验原密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getUserPwd())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }

        emailCodeService.verifyCode(EmailCodeScene.CHANGE_PASSWORD, user.getEmail(), request.getEmailCode(), true);

        // 新密码不能与原密码相同
        if (passwordEncoder.matches(request.getNewPassword(), user.getUserPwd())) {
            throw new BusinessException("新密码不能与原密码相同");
        }

        // 更新密码
        user.setUserPwd(passwordEncoder.encode(request.getNewPassword()));
        boolean updated = userRepository.updateById(user) > 0;
        if (updated) {
            redisUtil.deleteTokenByUserId(userId);
            redisUtil.deleteLoginUser("user:details:" + user.getUserName());
        }
        return updated;
    }

    @Override
    public void sendChangePasswordEmailCode(Long userId) {
        SysUser user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new BusinessException("当前账号未绑定邮箱，请先绑定邮箱");
        }
        emailCodeService.sendCode(EmailCodeScene.CHANGE_PASSWORD, user.getEmail());
    }

    @Override
    public void sendChangeEmailCode(Long userId, String email) {
        SysUser user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!StringUtils.hasText(email)) {
            throw new BusinessException("邮箱不能为空");
        }
        String normalizedEmail = email.trim().toLowerCase();

        SysUser existEmailUser = userRepository.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getEmail, normalizedEmail)
                        .ne(SysUser::getId, userId)
        );
        if (existEmailUser != null) {
            throw new BusinessException("该邮箱已被其他用户使用");
        }

        emailCodeService.sendCode(EmailCodeScene.CHANGE_EMAIL, normalizedEmail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitDesignerApply(Long userId, DesignerApplySubmitRequest request) {
        SysUser user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (hasDesignerRole(userId)) {
            throw new BusinessException("您已是设计者，无需重复申请");
        }

        DesignerApplyRequest pendingRequest = designerApplyRequestRepository.findPendingByUserId(userId);
        if (pendingRequest != null) {
            throw new BusinessException("已有待审核申请，请耐心等待管理员处理");
        }

        DesignerApplyRequest latestRequest = designerApplyRequestRepository.findLatestByUserId(userId);
        if (latestRequest != null
                && DesignerApplyStatus.REJECTED.getCode().equals(latestRequest.getStatus())
                && latestRequest.getRetryAfter() != null
                && latestRequest.getRetryAfter().isAfter(LocalDateTime.now())) {
            throw new BusinessException("申请被驳回后24小时内不可重复提交");
        }

        DesignerApplyRequest apply = new DesignerApplyRequest();
        apply.setUserId(user.getId());
        apply.setUserName(user.getUserName());
        apply.setNickname(user.getNickname());
        apply.setMobile(user.getMobile());
        apply.setEmail(user.getEmail());
        apply.setApplyReason(request.getApplyReason().trim());
        apply.setAttachmentUrls(StringUtils.hasText(request.getAttachmentUrls()) ? request.getAttachmentUrls().trim() : null);
        apply.setStatus(DesignerApplyStatus.PENDING.getCode());
        apply.setRequestTime(LocalDateTime.now());
        designerApplyRequestRepository.insert(apply);
    }

    @Override
    public MyDesignerApplyStatusVO getMyDesignerApplyStatus(Long userId) {
        MyDesignerApplyStatusVO result = new MyDesignerApplyStatusVO();
        result.setAlreadyDesigner(hasDesignerRole(userId));
        result.setServerTime(LocalDateTime.now());

        DesignerApplyRequest latestRequest = designerApplyRequestRepository.findLatestByUserId(userId);
        result.setLatestApply(latestRequest == null ? null : convertToDesignerApplyRequestVO(latestRequest));
        return result;
    }

    @Override
    public PageResult<DesignerApplyRequestVO> getDesignerApplyRequests(String status, Integer pageNum, Integer pageSize) {
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;

        LambdaQueryWrapper<DesignerApplyRequest> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(DesignerApplyRequest::getStatus, status.trim().toLowerCase());
        }
        queryWrapper.orderByDesc(DesignerApplyRequest::getRequestTime);

        Page<DesignerApplyRequest> page = new Page<>(currentPage, currentSize);
        designerApplyRequestRepository.selectPage(page, queryWrapper);

        List<DesignerApplyRequestVO> records = page.getRecords().stream()
                .map(this::convertToDesignerApplyRequestVO)
                .collect(Collectors.toList());

        return PageResult.<DesignerApplyRequestVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewDesignerApply(Long reviewerId, DesignerApplyReviewRequest request) {
        DesignerApplyRequest apply = designerApplyRequestRepository.selectById(request.getId());
        if (apply == null) {
            throw new BusinessException("设计者申请不存在");
        }
        if (!DesignerApplyStatus.PENDING.getCode().equals(apply.getStatus())) {
            throw new BusinessException("该申请已处理，请勿重复审核");
        }

        DesignerApplyStatus status = request.getStatus();
        if (status != DesignerApplyStatus.APPROVED && status != DesignerApplyStatus.REJECTED) {
            throw new BusinessException("无效的审核状态");
        }

        apply.setStatus(status.getCode());
        apply.setReviewBy(reviewerId);
        apply.setReviewTime(LocalDateTime.now());
        apply.setReviewRemark(request.getReviewRemark());

        SysUser user = userRepository.selectById(apply.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (status == DesignerApplyStatus.APPROVED) {
            grantDesignerRoleIfAbsent(user.getId());
            apply.setRetryAfter(null);
            redisUtil.deleteTokenByUserId(user.getId());
            redisUtil.deleteLoginUser("user:details:" + user.getUserName());
            sendDesignerApplyResultEmail(user, true, request.getReviewRemark(), null);
        } else {
            LocalDateTime retryAfter = LocalDateTime.now().plusHours(24);
            apply.setRetryAfter(retryAfter);
            sendDesignerApplyResultEmail(user, false, request.getReviewRemark(), retryAfter);
        }

        designerApplyRequestRepository.updateById(apply);
    }

    @Override
    public boolean requestDeletion(Long userId, String reason) {
        // 检查用户是否存在
        SysUser user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 检查是否已有待处理的删除请求
        UserDeletionRequest existingRequest = deletionRequestRepository.findByUserIdAndStatus(userId, DeletionStatus.PENDING);
        if (existingRequest != null) {
            throw new BusinessException(ResultCode.DELETION_REQUEST_EXISTS);
        }

        // 创建删除请求
        UserDeletionRequest deletionRequest = new UserDeletionRequest();
        deletionRequest.setUserId(userId);
        deletionRequest.setUsername(user.getUserName());
        deletionRequest.setPhone(user.getMobile());
        deletionRequest.setReason(reason);
        deletionRequest.setStatus(DeletionStatus.PENDING.getCode());
        deletionRequest.setRequestTime(LocalDateTime.now());

        // 保存删除请求
        return deletionRequestRepository.insert(deletionRequest) > 0;
    }

    @Override
    public PageResult<DeletionRequestVO> getDeletionRequests(String status, Integer pageNum, Integer pageSize) {
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        LambdaQueryWrapper<UserDeletionRequest> queryWrapper = new LambdaQueryWrapper<>();

        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(UserDeletionRequest::getStatus, DeletionStatus.valueOf(status.toUpperCase()).getCode());
        }
        queryWrapper.orderByDesc(UserDeletionRequest::getRequestTime);

        Page<UserDeletionRequest> page = new Page<>(currentPage, currentSize);
        deletionRequestRepository.selectPage(page, queryWrapper);

        List<DeletionRequestVO> records = page.getRecords().stream().map(this::convertToDeletionRequestVO).collect(Collectors.toList());

        return PageResult.<DeletionRequestVO>builder()
                .records(records)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    @Override
    public boolean approveDeletion(DeletionApprovalRequest request) {
        // 查询删除请求
        UserDeletionRequest deletionRequest = deletionRequestRepository.selectById(request.getId());
        if (deletionRequest == null) {
            throw new BusinessException(ResultCode.DELETION_REQUEST_NOT_FOUND);
        }

        // 更新请求状态
        deletionRequest.setStatus(request.getStatus().getCode());
        deletionRequest.setApprovalTime(LocalDateTime.now());
        deletionRequest.setAdminNote(request.getAdminNote());

        // 保存更新
        boolean updated = deletionRequestRepository.updateById(deletionRequest) > 0;

        // 如果请求被批准，标记用户为逻辑删除
        if (updated && request.getStatus() == DeletionStatus.APPROVED) {
            SysUser user = userRepository.selectById(deletionRequest.getUserId());
            if (user != null) {
                // 先禁用用户
                user.setStatus(0);
                userRepository.updateById(user);
                // 再用 deleteById 触发 @TableLogic 逻辑删除（UPDATE SET is_delete=1）
                userRepository.deleteById(deletionRequest.getUserId());
            }
        }

        return updated;
    }

    @Override
    public boolean updateUserStatus(Long userId, Integer status) {
        // 查询用户信息
        SysUser user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 验证状态值
        if (status != 0 && status != 1) {
            throw new BusinessException("无效的状态值");
        }

        // 更新用户状态
        user.setStatus(status);
        return userRepository.updateById(user) > 0;
    }

    /**
     * 将SysUser转换为UserListVO（包含隐私信息）
     */
    private UserListVO convertToVO(SysUser user) {
        UserListVO vo = new UserListVO();
        vo.setId(user.getId());
        vo.setUserName(user.getUserName());
        vo.setNickname(user.getNickname());
        vo.setSex(user.getSex());
        vo.setMobile(user.getMobile());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setAvatar(user.getAvatar());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    /**
     * 将UserDeletionRequest转换为DeletionRequestVO
     */
    private DeletionRequestVO convertToDeletionRequestVO(UserDeletionRequest request) {
        DeletionRequestVO vo = new DeletionRequestVO();
        vo.setId(request.getId());
        vo.setUserId(request.getUserId());
        vo.setUsername(request.getUsername());
        vo.setPhone(request.getPhone());
        vo.setReason(request.getReason());
        vo.setStatus(request.getStatus());
        vo.setRequestTime(request.getRequestTime());
        vo.setApprovalTime(request.getApprovalTime());
        vo.setAdminNote(request.getAdminNote());
        return vo;
    }

    private DesignerApplyRequestVO convertToDesignerApplyRequestVO(DesignerApplyRequest request) {
        DesignerApplyRequestVO vo = new DesignerApplyRequestVO();
        vo.setId(request.getId());
        vo.setUserId(request.getUserId());
        vo.setUserName(request.getUserName());
        vo.setNickname(request.getNickname());
        vo.setMobile(request.getMobile());
        vo.setEmail(request.getEmail());
        vo.setApplyReason(request.getApplyReason());
        vo.setAttachmentUrls(request.getAttachmentUrls());
        vo.setStatus(request.getStatus());
        vo.setRequestTime(request.getRequestTime());
        vo.setReviewTime(request.getReviewTime());
        vo.setReviewBy(request.getReviewBy());
        vo.setReviewRemark(request.getReviewRemark());
        vo.setRetryAfter(request.getRetryAfter());
        return vo;
    }

    private boolean hasDesignerRole(Long userId) {
        SysRole designerRole = roleRepository.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleName, "ROLE_DESIGNER")
                .eq(SysRole::getStatus, 1));
        if (designerRole == null) {
            return false;
        }

        return userRoleRepository.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleId, designerRole.getId())) > 0;
    }

    private void grantDesignerRoleIfAbsent(Long userId) {
        SysRole designerRole = roleRepository.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleName, "ROLE_DESIGNER")
                .eq(SysRole::getStatus, 1));
        if (designerRole == null) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }

        Long relationCount = userRoleRepository.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleId, designerRole.getId()));
        if (relationCount != null && relationCount > 0) {
            return;
        }

        SysUserRole relation = new SysUserRole();
        relation.setUserId(userId);
        relation.setRoleId(designerRole.getId());
        userRoleRepository.insert(relation);
    }

    private void sendDesignerApplyResultEmail(SysUser user, boolean approved, String reviewRemark, LocalDateTime retryAfter) {
        if (user == null || !StringUtils.hasText(user.getEmail()) || !StringUtils.hasText(mailFrom)) {
            return;
        }
        try {
            String userName = StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUserName();
            String resultText = approved ? "已通过" : "未通过";
            String remarkText = StringUtils.hasText(reviewRemark) ? reviewRemark : "无";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(user.getEmail());
            message.setSubject(approved ? "设计者申请审核通过" : "设计者申请审核结果通知");
            StringBuilder content = new StringBuilder();
            content.append("您好，").append(userName).append("\n\n")
                    .append("您的设计者申请审核结果：").append(resultText).append("。\n")
                    .append("审核备注：").append(remarkText).append("\n");

            if (approved) {
                content.append("\n您已获得设计者权限，请重新登录后使用相关功能。\n");
            } else if (retryAfter != null) {
                content.append("\n您可在 ").append(retryAfter).append(" 后重新提交申请。\n");
            }
            content.append("\n3DShop 团队");
            message.setText(content.toString());
            mailSender.send(message);
        } catch (Exception ignored) {
        }
    }

    @Override
    public AdminStatsVO getAdminStats() {
        AdminStatsVO vo = new AdminStatsVO();

        // 获取管理员角色ID
        SysRole adminRole = roleRepository.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleName, "ROLE_ADMIN")
                        .eq(SysRole::getStatus, 1));

        if (adminRole == null) {
            vo.setTotalAdmins(0L);
            vo.setActiveAdmins(0L);
            vo.setDisabledAdmins(0L);
            vo.setWeeklyLoginCount(0L);
            return vo;
        }

        // 查询管理员用户ID列表
        List<SysUserRole> adminUserRoles = userRoleRepository.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getRoleId, adminRole.getId()));

        List<Long> adminUserIds = adminUserRoles.stream()
                .map(SysUserRole::getUserId)
                .collect(Collectors.toList());

        if (adminUserIds.isEmpty()) {
            vo.setTotalAdmins(0L);
            vo.setActiveAdmins(0L);
            vo.setDisabledAdmins(0L);
            vo.setWeeklyLoginCount(0L);
            return vo;
        }

        // 查询管理员总数
        vo.setTotalAdmins((long) adminUserIds.size());

        // 查询启用的管理员数
        Long activeCount = userRepository.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .in(SysUser::getId, adminUserIds)
                        .eq(SysUser::getStatus, 1)
                        .eq(SysUser::getIsDelete, 0));
        vo.setActiveAdmins(activeCount);

        // 查询禁用的管理员数
        Long disabledCount = userRepository.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .in(SysUser::getId, adminUserIds)
                        .eq(SysUser::getStatus, 0)
                        .eq(SysUser::getIsDelete, 0));
        vo.setDisabledAdmins(disabledCount);

        // 暂不统计登录数，返回0
        vo.setWeeklyLoginCount(0L);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResultVO batchReviewDesignerApply(Long operatorId, BatchReviewRequest request) {
        List<BatchOperationResultVO.FailureDetail> failures = new java.util.ArrayList<>();
        int successCount = 0;

        for (Long applyId : request.getIds()) {
            try {
                DesignerApplyRequest apply = designerApplyRequestRepository.selectById(applyId);
                if (apply == null) {
                    failures.add(BatchOperationResultVO.FailureDetail.builder()
                            .id(applyId)
                            .reason("申请不存在")
                            .build());
                    continue;
                }
                if (!DesignerApplyStatus.PENDING.getCode().equals(apply.getStatus())) {
                    failures.add(BatchOperationResultVO.FailureDetail.builder()
                            .id(applyId)
                            .reason("申请状态不允许审核")
                            .build());
                    continue;
                }

                // 执行审核逻辑
                DesignerApplyReviewRequest reviewRequest = new DesignerApplyReviewRequest();
                reviewRequest.setId(applyId);
                reviewRequest.setStatus("APPROVED".equals(request.getReviewStatus())
                        ? DesignerApplyStatus.APPROVED
                        : DesignerApplyStatus.REJECTED);
                reviewRequest.setReviewRemark(request.getReviewRemark());

                reviewDesignerApply(operatorId, reviewRequest);
                successCount++;
            } catch (Exception e) {
                failures.add(BatchOperationResultVO.FailureDetail.builder()
                        .id(applyId)
                        .reason(e.getMessage())
                        .build());
            }
        }

        return BatchOperationResultVO.partial(request.getIds().size(), successCount, failures);
    }
}
