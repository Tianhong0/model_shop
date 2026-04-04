package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 对话日志实体
 */
@Data
@TableName("ai_chat_log")
public class AiChatLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 用户消息
     */
    private String userMessage;

    /**
     * AI回复
     */
    private String aiResponse;

    /**
     * 意图类型
     */
    private String intent;

    /**
     * 置信度
     */
    private BigDecimal confidence;

    /**
     * AI提供商
     */
    private String provider;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * 消耗Token数
     */
    private Integer tokensUsed;

    /**
     * 响应时间(毫秒)
     */
    private Integer responseTimeMs;

    /**
     * 是否转人工
     */
    private Integer isTransferred;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
