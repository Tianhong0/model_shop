-- 分享推广模块数据库表
-- 执行顺序：1

-- 1. 用户邀请码表
CREATE TABLE IF NOT EXISTS `user_invite_code` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `invite_code` VARCHAR(16) NOT NULL COMMENT '邀请码（唯一）',
    `total_invited` INT DEFAULT 0 COMMENT '累计邀请人数',
    `total_points_earned` INT DEFAULT 0 COMMENT '累计获得积分',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-正常, 0-禁用',
    `is_delete` TINYINT DEFAULT 0 COMMENT '逻辑删除：1-已删, 0-未删',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_invite_code` (`invite_code`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_invite_code` (`invite_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户邀请码表';

-- 2. 邀请关系表
CREATE TABLE IF NOT EXISTS `invite_relation` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `inviter_id` BIGINT NOT NULL COMMENT '邀请人ID',
    `invitee_id` BIGINT NOT NULL COMMENT '被邀请人ID',
    `invite_code` VARCHAR(16) NOT NULL COMMENT '使用的邀请码',
    `register_time` DATETIME NOT NULL COMMENT '注册时间',
    `first_order_time` DATETIME DEFAULT NULL COMMENT '首单时间',
    `first_order_id` BIGINT DEFAULT NULL COMMENT '首单订单ID',
    `total_order_count` INT DEFAULT 0 COMMENT '累计订单数',
    `total_order_amount` DECIMAL(12,2) DEFAULT 0.00 COMMENT '累计订单金额',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-正常, 0-无效',
    `is_delete` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_invitee_id` (`invitee_id`),
    KEY `idx_inviter_id` (`inviter_id`),
    KEY `idx_invite_code` (`invite_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请关系表';

-- 3. 推广奖励记录表
CREATE TABLE IF NOT EXISTS `promotion_reward` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '获得奖励的用户ID（邀请人）',
    `invite_relation_id` BIGINT DEFAULT NULL COMMENT '关联邀请关系ID',
    `reward_type` VARCHAR(32) NOT NULL COMMENT '奖励类型：INVITE_REGISTER-邀请注册, FIRST_ORDER-首单奖励, CONSUME_REBATE-消费返积分',
    `reward_points` INT NOT NULL COMMENT '奖励积分',
    `ref_type` VARCHAR(32) DEFAULT NULL COMMENT '关联类型：ORDER-订单, REGISTER-注册',
    `ref_id` BIGINT DEFAULT NULL COMMENT '关联ID',
    `ref_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '关联金额',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-已发放, 0-待发放, 2-已取消',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `is_delete` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_reward_type` (`reward_type`),
    KEY `idx_ref_id` (`ref_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推广奖励记录表';

-- 4. 推广分享记录表
CREATE TABLE IF NOT EXISTS `promotion_share` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '分享用户ID',
    `share_type` VARCHAR(32) NOT NULL COMMENT '分享类型：MODEL-商品, POSTER-海报, LINK-链接',
    `share_channel` VARCHAR(32) DEFAULT NULL COMMENT '分享渠道：WECHAT-微信, MOMENTS-朋友圈, QQ, WEIBO-微博',
    `ref_type` VARCHAR(32) DEFAULT NULL COMMENT '关联类型：MODEL-商品, PAGE-页面',
    `ref_id` BIGINT DEFAULT NULL COMMENT '关联ID',
    `share_url` VARCHAR(512) DEFAULT NULL COMMENT '分享链接',
    `poster_url` VARCHAR(512) DEFAULT NULL COMMENT '海报图片URL',
    `click_count` INT DEFAULT 0 COMMENT '点击次数',
    `convert_count` INT DEFAULT 0 COMMENT '转化次数',
    `is_delete` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_share_type` (`share_type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推广分享记录表';

-- 5. 推广配置表
CREATE TABLE IF NOT EXISTS `promotion_config` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `config_key` VARCHAR(64) NOT NULL COMMENT '配置键',
    `config_value` VARCHAR(512) NOT NULL COMMENT '配置值',
    `config_desc` VARCHAR(255) DEFAULT NULL COMMENT '配置描述',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-启用, 0-禁用',
    `is_delete` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推广配置表';

-- 6. 修改用户表添加邀请相关字段
ALTER TABLE `sys_user`
ADD COLUMN `invite_code_id` BIGINT DEFAULT NULL COMMENT '邀请码ID' AFTER `avatar`,
ADD COLUMN `inviter_id` BIGINT DEFAULT NULL COMMENT '邀请人ID' AFTER `invite_code_id`;

-- 7. 初始化配置数据
INSERT INTO `promotion_config` (`id`, `config_key`, `config_value`, `config_desc`, `status`) VALUES
(1, 'INVITE_REGISTER_POINTS', '50', '邀请注册奖励积分', 1),
(2, 'FIRST_ORDER_POINTS', '100', '被邀请人首单奖励积分', 1),
(3, 'CONSUME_REBATE_RATE', '0.01', '消费返积分比例（1%）', 1),
(4, 'MAX_REBATE_POINTS', '500', '单笔订单最大返积分', 1),
(5, 'INVITE_CODE_LENGTH', '6', '邀请码长度', 1),
(6, 'PROMOTION_BASE_URL', '', '推广链接域名（为空则使用默认域名）', 1);
