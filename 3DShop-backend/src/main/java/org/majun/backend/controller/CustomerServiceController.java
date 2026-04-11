package org.majun.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.Result;
import org.majun.backend.dto.CsMessageSendRequest;
import org.majun.backend.entity.SysUser;
import org.majun.backend.repository.SysUserRepository;
import org.majun.backend.service.CustomerServiceService;
import org.majun.backend.util.JwtUtil;
import org.majun.backend.util.MinioUtil;
import org.majun.backend.vo.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/cs")
@RequiredArgsConstructor
/**
 * 客服系统控制器
 */
public class CustomerServiceController {

    private final CustomerServiceService customerServiceService;
    private final SysUserRepository sysUserRepository;
    private final JwtUtil jwtUtil;
    private final MinioUtil minioUtil;

    // ==================== 用户端 ====================

    /**
     * 用户发起客服咨询 - 创建新会话并分配客服
     */
    @PostMapping("/conversation/start")
    public Result<CsConversationVO> startConversation(
            @RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.fail(401, "未登录");
        }
        String jwt = token.substring(7);
        Long userId = jwtUtil.getUserId(jwt);

        CsConversationVO conversation = customerServiceService.startConversation(userId);
        return Result.success(conversation);
    }

    /**
     * 获取用户当前会话状态
     */
    @GetMapping("/conversation/status")
    public Result<CsConversationVO> getConversationStatus(
            @RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.fail(401, "未登录");
        }
        String jwt = token.substring(7);
        Long userId = jwtUtil.getUserId(jwt);

        CsConversationVO conversation = customerServiceService.getActiveConversation(userId);
        return Result.success(conversation);
    }

    /**
     * 获取会话消息历史 (分页)
     */
    @GetMapping("/messages/{conversationId}")
    public Result<PageResult<CsMessageVO>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        PageResult<CsMessageVO> messages = customerServiceService.getMessages(conversationId, pageNum, pageSize);
        return Result.success(messages);
    }

    /**
     * 用户发送消息
     */
    @PostMapping("/message/send")
    public Result<CsMessageVO> sendMessage(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody CsMessageSendRequest request) {
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.fail(401, "未登录");
        }
        String jwt = token.substring(7);
        Long userId = jwtUtil.getUserId(jwt);

        CsMessageVO message = customerServiceService.sendMessage(userId, request);
        return Result.success(message);
    }

    /**
     * 用户标记消息已读
     */
    @PutMapping("/message/read/{conversationId}")
    public Result<Void> markMessagesAsRead(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long conversationId) {
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.fail(401, "未登录");
        }
        String jwt = token.substring(7);
        Long userId = jwtUtil.getUserId(jwt);

        customerServiceService.markUserAsRead(userId, conversationId);
        return Result.success();
    }

    /**
     * 用户结束会话
     */
    @PostMapping("/conversation/end/{conversationId}")
    public Result<Void> endConversation(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long conversationId) {
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.fail(401, "未登录");
        }
        String jwt = token.substring(7);
        Long userId = jwtUtil.getUserId(jwt);

        customerServiceService.endConversation(userId, conversationId, "USER_END");
        return Result.success();
    }

    /**
     * 获取在线客服列表
     */
    @GetMapping("/online-admins")
    public Result<List<CsAdminStatusVO>> getOnlineAdmins() {
        List<CsAdminStatusVO> admins = customerServiceService.getOnlineAdmins();
        return Result.success(admins);
    }

    // ==================== 管理员端 ====================

    /**
     * 客服获取会话列表
     */
    @GetMapping("/admin/conversations")
    public Result<PageResult<CsConversationVO>> getAdminConversations(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.fail(401, "未登录");
        }
        String jwt = token.substring(7);
        Long adminId = jwtUtil.getUserId(jwt);

        PageResult<CsConversationVO> conversations = customerServiceService.getAdminConversations(
            adminId, pageNum, pageSize, status);
        return Result.success(conversations);
    }

    /**
     * 客服接单/开始会话
     */
    @PostMapping("/admin/conversation/accept/{conversationId}")
    public Result<CsConversationVO> acceptConversation(
            @RequestHeader(value = "Authorization") String token,
            @PathVariable Long conversationId) {
        String jwt = token.substring(7);
        Long adminId = jwtUtil.getUserId(jwt);

        CsConversationVO conversation = customerServiceService.acceptConversation(adminId, conversationId);
        return Result.success(conversation);
    }

    /**
     * 客服发送消息
     */
    @PostMapping("/admin/message/send")
    public Result<CsMessageVO> adminSendMessage(
            @RequestHeader(value = "Authorization") String token,
            @Valid @RequestBody CsMessageSendRequest request) {
        String jwt = token.substring(7);
        Long adminId = jwtUtil.getUserId(jwt);

        CsMessageVO message = customerServiceService.adminSendMessage(adminId, request);
        return Result.success(message);
    }

    /**
     * 客服结束会话
     */
    @PostMapping("/admin/conversation/end/{conversationId}")
    public Result<Void> adminEndConversation(
            @RequestHeader(value = "Authorization") String token,
            @PathVariable Long conversationId,
            @RequestParam(required = false) String reason) {
        String jwt = token.substring(7);
        Long adminId = jwtUtil.getUserId(jwt);

        customerServiceService.endConversationByAdmin(adminId, conversationId, reason);
        return Result.success();
    }

    /**
     * 客服设置在线状态
     */
    @PostMapping("/admin/status")
    public Result<Void> setAdminStatus(
            @RequestHeader(value = "Authorization") String token,
            @RequestParam Boolean online) {
        String jwt = token.substring(7);
        Long adminId = jwtUtil.getUserId(jwt);

        SysUser admin = sysUserRepository.selectById(adminId);
        String nickname = admin != null ? admin.getNickname() : "客服";

        customerServiceService.setAdminOnlineStatus(adminId, nickname, online);
        return Result.success();
    }

    /**
     * 客服心跳保活
     */
    @PostMapping("/admin/heartbeat")
    public Result<Void> adminHeartbeat(
            @RequestHeader(value = "Authorization") String token) {
        String jwt = token.substring(7);
        Long adminId = jwtUtil.getUserId(jwt);

        customerServiceService.updateAdminHeartbeat(adminId);
        return Result.success();
    }

    /**
     * 客服标记消息已读
     */
    @PutMapping("/admin/message/read/{conversationId}")
    public Result<Void> adminMarkAsRead(
            @RequestHeader(value = "Authorization") String token,
            @PathVariable Long conversationId) {
        String jwt = token.substring(7);
        Long adminId = jwtUtil.getUserId(jwt);

        customerServiceService.markAdminAsRead(adminId, conversationId);
        return Result.success();
    }

    /**
     * 管理员查看聊天历史
     */
    @GetMapping("/admin/history/{conversationId}")
    public Result<CsConversationDetailVO> getConversationHistory(
            @PathVariable Long conversationId) {
        CsConversationDetailVO detail = customerServiceService.getConversationDetail(conversationId);
        return Result.success(detail);
    }

    /**
     * 上传客服聊天媒体文件（图片/视频）
     */
    @PostMapping("/upload")
    public Result<String> uploadMedia(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam("file") MultipartFile file) {
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.fail(401, "未登录");
        }

        if (file == null || file.isEmpty()) {
            return Result.fail(400, "文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return Result.fail(400, "文件名无效");
        }

        String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        // 只允许图片和视频
        if (!ext.matches("\\.(jpg|jpeg|png|gif|webp|mp4|webm)$")) {
            return Result.fail(400, "不支持的文件类型");
        }

        String folder = ext.matches("\\.(mp4|webm)$") ? "cs-video" : "cs-image";

        try {
            String url = minioUtil.uploadFile(file, folder);
            return Result.success(url);
        } catch (Exception e) {
            log.error("上传客服媒体文件失败", e);
            return Result.fail(500, "上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取客服统计数据
     */
    @GetMapping("/admin/stats")
    public Result<CsStatsVO> getStats() {
        return Result.success(customerServiceService.getStats());
    }
}
