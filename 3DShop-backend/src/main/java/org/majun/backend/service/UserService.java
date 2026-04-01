package org.majun.backend.service;

import org.majun.backend.dto.ChangePasswordRequest;
import org.majun.backend.dto.DeletionApprovalRequest;
import org.majun.backend.dto.DesignerApplyReviewRequest;
import org.majun.backend.dto.DesignerApplySubmitRequest;
import org.majun.backend.dto.UserQueryRequest;
import org.majun.backend.dto.UserUpdateRequest;
import org.majun.backend.dto.BatchReviewRequest;
import org.majun.backend.vo.AdminStatsVO;
import org.majun.backend.vo.BatchOperationResultVO;
import org.majun.backend.vo.DeletionRequestVO;
import org.majun.backend.vo.DesignerApplyRequestVO;
import org.majun.backend.vo.DesignerVO;
import org.majun.backend.vo.MyDesignerApplyStatusVO;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.UserListVO;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户列表查询
     *
     * @param request 查询请求
     * @return 用户列表
     */
    PageResult<UserListVO> getUserList(UserQueryRequest request);

    /**
     * 用户详情查询
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    UserListVO getUserDetail(Long userId);

    /**
     * 设计者列表查询
     *
     * @return 设计者列表
     */
    List<DesignerVO> getDesignerList();

    /**
     * 更新用户信息
     *
     * @param request 用户更新请求
     * @return 更新结果
     */
    boolean updateUser(UserUpdateRequest request);

    /**
     * 修改密码
     *
     * @param userId  用户ID
     * @param request 修改密码请求
     * @return 修改结果
     */
    boolean changePassword(Long userId, ChangePasswordRequest request);

    /**
     * 发送修改密码邮箱验证码
     */
    void sendChangePasswordEmailCode(Long userId);

    /**
     * 发送修改邮箱验证码
     */
    void sendChangeEmailCode(Long userId, String email);

    /**
     * 用户提交设计者申请
     */
    void submitDesignerApply(Long userId, DesignerApplySubmitRequest request);

    /**
     * 获取当前用户的设计者申请状态
     */
    MyDesignerApplyStatusVO getMyDesignerApplyStatus(Long userId);

    /**
     * 管理员分页查询设计者申请
     */
    PageResult<DesignerApplyRequestVO> getDesignerApplyRequests(String status, Integer pageNum, Integer pageSize);

    /**
     * 管理员审核设计者申请
     */
    void reviewDesignerApply(Long reviewerId, DesignerApplyReviewRequest request);

    /**
     * 申请用户注销
     *
     * @param userId 用户ID
     * @param reason 注销原因
     * @return 申请结果
     */
    boolean requestDeletion(Long userId, String reason);

    /**
     * 获取用户注销申请列表
     *
     * @param status 请求状态
     * @return 注销申请列表
     */
    PageResult<DeletionRequestVO> getDeletionRequests(String status, Integer pageNum, Integer pageSize);

    /**
     * 审批注销申请
     *
     * @param request 审批请求
     * @return 审批结果
     */
    boolean approveDeletion(DeletionApprovalRequest request);

    /**
     * 更新用户状态
     *
     * @param userId 用户ID
     * @param status 状态：1-正常, 0-禁用
     * @return 更新结果
     */
    boolean updateUserStatus(Long userId, Integer status);

    /**
     * 获取管理员统计信息
     *
     * @return 管理员统计
     */
    AdminStatsVO getAdminStats();

    /**
     * 批量审核设计者申请
     *
     * @param operatorId 操作人ID
     * @param request    批量审核请求
     * @return 批量操作结果
     */
    BatchOperationResultVO batchReviewDesignerApply(Long operatorId, BatchReviewRequest request);
}
