package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.majun.backend.entity.CsConversation;

import java.util.List;

@Mapper
public interface CsConversationRepository extends BaseMapper<CsConversation> {

    /**
     * 获取用户当前进行中的会话
     */
    CsConversation findActiveByUserId(Long userId);

    /**
     * 获取等待分配的会话列表
     */
    List<CsConversation> findWaitingConversations();

    /**
     * 获取客服当前进行中的会话数
     */
    int countActiveByAdminId(Long adminId);

    /**
     * 根据状态查询会话列表
     */
    List<CsConversation> findByStatus(Integer status);

    /**
     * 查询超时的进行中会话（SQL层过滤）
     */
    List<CsConversation> findExpiredConversations(@Param("timeoutMinutes") long timeoutMinutes);
}
