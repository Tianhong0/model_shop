package org.majun.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.majun.backend.common.Result;
import org.majun.backend.dto.*;
import org.majun.backend.security.LoginUser;
import org.majun.backend.service.UserService;
import org.majun.backend.vo.DeletionRequestVO;
import org.majun.backend.vo.DesignerApplyRequestVO;
import org.majun.backend.vo.DesignerVO;
import org.majun.backend.vo.MyDesignerApplyStatusVO;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.UserListVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器
 */
@Tag(name = "用户管理", description = "用户相关接口")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户列表查询（仅管理员可用）
     */
    @Operation(summary = "用户列表查询", description = "查询系统用户列表，仅管理员可访问")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/list")
    public Result<PageResult<UserListVO>> getUserList(@Valid UserQueryRequest request) {
        PageResult<UserListVO> userList = userService.getUserList(request);
        return Result.success("查询成功", userList);
    }

    /**
     * 用户详情查询（管理员或本人可访问）
     */
    @Operation(summary = "用户详情查询", description = "查询用户详情，管理员或本人可访问")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #userId == authentication.principal.id")
    @GetMapping("/{userId}")
    public Result<UserListVO> getUserDetail(@PathVariable Long userId) {
        UserListVO userDetail = userService.getUserDetail(userId);
        return Result.success("查询成功", userDetail);
    }

    /**
     * 设计者列表查询（所有人可访问）
     */
    @Operation(summary = "设计者列表查询", description = "查询设计者列表，不包含隐私信息")
    @GetMapping("/designers")
    public Result<List<DesignerVO>> getDesignerList() {
        List<DesignerVO> designerList = userService.getDesignerList();
        return Result.success("查询成功", designerList);
    }

    /**
     * 修改个人信息（仅本人可操作）
     */
    @Operation(summary = "修改个人信息", description = "用户修改自己的个人信息")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@AuthenticationPrincipal LoginUser loginUser,
                                      @Valid @RequestBody UserUpdateRequest request) {
        // 强制设置为当前登录用户的ID，防止篡改
        request.setId(loginUser.getId());
        boolean result = userService.updateUser(request);
        return result ? Result.success("修改成功") : Result.fail("修改失败");
    }

    /**
     * 修改密码（仅本人可操作）
     */
    @Operation(summary = "修改密码", description = "用户修改自己的密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@AuthenticationPrincipal LoginUser loginUser,
                                       @Valid @RequestBody ChangePasswordRequest request) {
        boolean result = userService.changePassword(loginUser.getId(), request);
        return result ? Result.success("密码修改成功") : Result.fail("密码修改失败");
    }

    @Operation(summary = "发送修改密码邮箱验证码", description = "用户修改密码前发送邮箱验证码")
    @PostMapping("/password/email-code")
    public Result<Void> sendChangePasswordEmailCode(@AuthenticationPrincipal LoginUser loginUser) {
        userService.sendChangePasswordEmailCode(loginUser.getId());
        return Result.success("验证码已发送，请注意查收邮箱");
    }

    @Operation(summary = "发送修改邮箱验证码", description = "用户修改个人资料邮箱前发送验证码")
    @PostMapping("/profile/email-code")
    public Result<Void> sendChangeEmailCode(@AuthenticationPrincipal LoginUser loginUser,
                                            @Valid @RequestBody EmailCodeSendRequest request) {
        userService.sendChangeEmailCode(loginUser.getId(), request.getEmail());
        return Result.success("验证码已发送，请注意查收邮箱");
    }

    @Operation(summary = "提交设计者申请", description = "普通用户提交成为设计者的申请")
    @PostMapping("/designer-apply")
    public Result<Void> submitDesignerApply(@AuthenticationPrincipal LoginUser loginUser,
                                            @Valid @RequestBody DesignerApplySubmitRequest request) {
        userService.submitDesignerApply(loginUser.getId(), request);
        return Result.success("申请已提交，请等待管理员审核");
    }

    @Operation(summary = "查询我的设计者申请状态", description = "查询当前登录用户最新设计者申请状态")
    @GetMapping("/designer-apply/my-status")
    public Result<MyDesignerApplyStatusVO> getMyDesignerApplyStatus(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(userService.getMyDesignerApplyStatus(loginUser.getId()));
    }

    @Operation(summary = "分页查询设计者申请", description = "管理员分页查询普通用户设计者申请")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/designer-apply/requests")
    public Result<PageResult<DesignerApplyRequestVO>> getDesignerApplyRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success("查询成功", userService.getDesignerApplyRequests(status, pageNum, pageSize));
    }

    @Operation(summary = "审核设计者申请", description = "管理员审核普通用户设计者申请")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/designer-apply/review")
    public Result<Void> reviewDesignerApply(@AuthenticationPrincipal LoginUser loginUser,
                                            @Valid @RequestBody DesignerApplyReviewRequest request) {
        userService.reviewDesignerApply(loginUser.getId(), request);
        return Result.success("审核完成");
    }

    /**
     * 提交注销申请（仅本人可操作）
     */
    @Operation(summary = "提交注销申请", description = "用户提交账户注销申请，需管理员审批")
    @PostMapping("/deletion-request")
    public Result<Void> requestDeletion(@AuthenticationPrincipal LoginUser loginUser,
                                        @Valid @RequestBody DeletionRequestDTO request) {
        boolean result = userService.requestDeletion(loginUser.getId(), request.getReason());
        return result ? Result.success("注销申请已提交，等待管理员审批") : Result.fail("申请提交失败");
    }

    /**
     * 查看注销申请列表（仅管理员可访问）
     */
    @Operation(summary = "查看注销申请列表", description = "管理员查看用户注销申请列表")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/deletion-requests")
    public Result<PageResult<DeletionRequestVO>> getDeletionRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<DeletionRequestVO> requests = userService.getDeletionRequests(status, pageNum, pageSize);
        return Result.success("查询成功", requests);
    }

    /**
     * 审批注销申请（仅管理员可操作）
     */
    @Operation(summary = "审批注销申请", description = "管理员审批用户注销申请")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/deletion-approval")
    public Result<Void> approveDeletion(@Valid @RequestBody DeletionApprovalRequest request) {
        boolean result = userService.approveDeletion(request);
        return result ? Result.success("审批完成") : Result.fail("审批失败");
    }

    /**
     * 更新用户状态（仅管理员可操作）
     */
    @Operation(summary = "更新用户状态", description = "管理员更新用户状态：启用/禁用")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/status")
    public Result<Void> updateUserStatus(@RequestParam Long userId, @RequestParam Integer status) {
        boolean result = userService.updateUserStatus(userId, status);
        return result ? Result.success("状态更新成功") : Result.fail("状态更新失败");
    }
}
