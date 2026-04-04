package org.majun.backend.service;

import org.majun.backend.dto.CsMessageSendRequest;
import org.majun.backend.vo.CsAdminStatusVO;
import org.majun.backend.vo.CsConversationDetailVO;
import org.majun.backend.vo.CsConversationVO;
import org.majun.backend.vo.CsMessageVO;
import org.majun.backend.vo.CsStatsVO;
import org.majun.backend.vo.PageResult;

public interface CustomerServiceService {

    // ==================== 用户端 ====================

    /**
     * 用户发起客服咨询 - 创建新会话并分配客服
     */
    CsConversationVO startConversation(Long userId);

    /**
     * 获取用户当前进行中的会话
     */
    CsConversationVO getActiveConversation(Long userId);

    /**
     * 获取会话消息历史 (分页)
     */
    PageResult<CsMessageVO> getMessages(Long conversationId, Integer pageNum, Integer pageSize);

    /**
     * 用户发送消息
     */
    CsMessageVO sendMessage(Long userId, CsMessageSendRequest request);

    /**
     * 用户标记消息已读
     */
    void markUserAsRead(Long userId, Long conversationId);

    /**
     * 用户结束会话
     */
    void endConversation(Long userId, Long conversationId, String reason);

    /**
     * 获取在线客服列表
     */
    java.util.List<CsAdminStatusVO> getOnlineAdmins();

    // ==================== 管理员端 ====================

    /**
     * 客服获取会话列表 (分页)
     */
    PageResult<CsConversationVO> getAdminConversations(Long adminId, Integer pageNum, Integer pageSize, Integer status);

    /**
     * 客服接单/开始会话
     */
    CsConversationVO acceptConversation(Long adminId, Long conversationId);

    /**
     * 客服发送消息
     */
    CsMessageVO adminSendMessage(Long adminId, CsMessageSendRequest request);

    /**
     * 客服结束会话
     */
    void endConversationByAdmin(Long adminId, Long conversationId, String reason);

    /**
     * 客服设置在线状态
     */
    void setAdminOnlineStatus(Long adminId, String nickname, boolean online);

    /**
     * 客服心跳保活
     */
    void updateAdminHeartbeat(Long adminId);

    /**
     * 客服标记消息已读
     */
    void markAdminAsRead(Long adminId, Long conversationId);

    /**
     * 获取会话详情（包含完整消息）
     */
    CsConversationDetailVO getConversationDetail(Long conversationId);

    /**
     * 自动结束超时会话（定时任务调用）
     */
    void closeExpiredConversations();

    /**
     * 获取客服统计数据
     */
    CsStatsVO getStats();
}
