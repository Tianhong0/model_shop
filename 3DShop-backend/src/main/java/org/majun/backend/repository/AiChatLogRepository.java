package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.AiChatLog;

/**
 * AI 对话日志 Repository
 */
@Mapper
public interface AiChatLogRepository extends BaseMapper<AiChatLog> {
}
