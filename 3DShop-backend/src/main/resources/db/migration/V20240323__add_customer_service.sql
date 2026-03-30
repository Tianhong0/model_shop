-- 客服系统表结构
-- V20240323__add_customer_service.sql

-- 客服会话表
CREATE TABLE IF NOT EXISTS cs_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_no VARCHAR(64) NOT NULL COMMENT '会话编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    user_nickname VARCHAR(64) COMMENT '用户昵称',
    user_avatar VARCHAR(512) COMMENT '用户头像',
    admin_id BIGINT COMMENT '客服ID',
    admin_nickname VARCHAR(64) COMMENT '客服昵称',
    status INT NOT NULL DEFAULT 0 COMMENT '状态: 0-等待分配 1-进行中 2-已结束',
    unread_user_count INT DEFAULT 0 COMMENT '用户未读数',
    unread_admin_count INT DEFAULT 0 COMMENT '客服未读数',
    last_message_time DATETIME COMMENT '最后消息时间',
    end_reason VARCHAR(64) COMMENT '结束原因',
    end_time DATETIME COMMENT '结束时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服会话表';

-- 客服消息表
CREATE TABLE IF NOT EXISTS cs_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    sender_role VARCHAR(16) NOT NULL COMMENT '发送者角色: USER/ADMIN',
    sender_nickname VARCHAR(64) COMMENT '发送者昵称',
    message_type INT NOT NULL DEFAULT 1 COMMENT '消息类型: 1-文本 2-图片 3-视频 4-系统消息',
    content TEXT COMMENT '消息内容',
    attachments VARCHAR(1024) COMMENT '附件地址(图片/视频)',
    is_read INT DEFAULT 0 COMMENT '是否已读: 0-未读 1-已读',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服消息表';

-- 客服状态表
CREATE TABLE IF NOT EXISTS cs_admin_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id BIGINT NOT NULL UNIQUE COMMENT '管理员ID',
    admin_nickname VARCHAR(64) COMMENT '管理员昵称',
    is_online INT DEFAULT 0 COMMENT '是否在线: 0-离线 1-在线',
    current_conversation_count INT DEFAULT 0 COMMENT '当前会话数',
    total_served_count INT DEFAULT 0 COMMENT '累计服务数',
    last_heartbeat DATETIME COMMENT '最后心跳时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_admin_id (admin_id),
    INDEX idx_is_online (is_online)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服状态表';
