-- 悬赏评价与信誉系统 - 数据库迁移脚本

-- 1. 悬赏评价表
CREATE TABLE IF NOT EXISTS `bounty_rating` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `task_id` BIGINT NOT NULL COMMENT '任务ID',
  `publisher_id` BIGINT NOT NULL COMMENT '发布者ID',
  `designer_id` BIGINT NOT NULL COMMENT '设计者ID',
  `delivery_id` BIGINT DEFAULT NULL COMMENT '交付ID',
  `score` TINYINT NOT NULL COMMENT '评分:1-5星',
  `comment` VARCHAR(1000) DEFAULT NULL COMMENT '评价内容',
  `images` VARCHAR(2000) DEFAULT NULL COMMENT '评价图片,逗号分隔',
  `is_anonymous` TINYINT NOT NULL DEFAULT 0 COMMENT '是否匿名:0否,1是',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1正常,0无效(申诉通过后)',
  `admin_remark` VARCHAR(500) DEFAULT NULL COMMENT '管理员备注(申诉处理)',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_rating` (`task_id`),
  KEY `idx_designer_rating` (`designer_id`, `status`, `create_time`),
  KEY `idx_publisher_rating` (`publisher_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='悬赏评价表';

-- 2. 设计者信誉表
CREATE TABLE IF NOT EXISTS `designer_reputation` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `designer_id` BIGINT NOT NULL COMMENT '设计者ID',
  `reputation_score` INT NOT NULL DEFAULT 80 COMMENT '信誉评分(默认80分)',
  `total_tasks` INT NOT NULL DEFAULT 0 COMMENT '完成任务总数',
  `total_ratings` INT NOT NULL DEFAULT 0 COMMENT '被评价总数',
  `avg_score` DECIMAL(3,2) DEFAULT NULL COMMENT '平均评分',
  `five_star_count` INT NOT NULL DEFAULT 0 COMMENT '五星好评数',
  `four_star_count` INT NOT NULL DEFAULT 0 COMMENT '四星好评数',
  `three_star_count` INT NOT NULL DEFAULT 0 COMMENT '三星评价数',
  `two_star_count` INT NOT NULL DEFAULT 0 COMMENT '二星评价数',
  `one_star_count` INT NOT NULL DEFAULT 0 COMMENT '一星差评数',
  `quality_answer_count` INT NOT NULL DEFAULT 0 COMMENT '优质回答数',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_designer` (`designer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设计者信誉表';

-- 3. 评价申诉表
CREATE TABLE IF NOT EXISTS `bounty_rating_appeal` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `rating_id` BIGINT NOT NULL COMMENT '评价ID',
  `designer_id` BIGINT NOT NULL COMMENT '申诉设计者ID',
  `reason` VARCHAR(1000) NOT NULL COMMENT '申诉理由',
  `evidence` VARCHAR(2000) DEFAULT NULL COMMENT '证据材料URL,逗号分隔',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态:0待处理,1通过,2驳回',
  `admin_id` BIGINT DEFAULT NULL COMMENT '处理管理员ID',
  `admin_remark` VARCHAR(500) DEFAULT NULL COMMENT '处理备注',
  `processed_time` DATETIME DEFAULT NULL COMMENT '处理时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_rating_appeal` (`rating_id`, `status`),
  KEY `idx_designer_appeal` (`designer_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价申诉表';

-- 4. 悬赏交付文件表
CREATE TABLE IF NOT EXISTS `bounty_delivery_file` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `delivery_id` BIGINT NOT NULL COMMENT '交付ID',
  `file_url` VARCHAR(500) NOT NULL COMMENT '文件地址',
  `file_name` VARCHAR(255) DEFAULT NULL COMMENT '文件名称',
  `file_type` VARCHAR(30) DEFAULT NULL COMMENT '文件类型',
  `file_size` BIGINT DEFAULT NULL COMMENT '文件大小',
  `encrypted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否加密存储:0否,1是',
  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_delivery_file` (`delivery_id`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='悬赏交付文件表';

-- 5. 为bounty_delivery添加版权授权字段
ALTER TABLE `bounty_delivery`
  ADD COLUMN `allow_commercial_use` TINYINT NOT NULL DEFAULT 0 COMMENT '允许商业使用:0否,1是' AFTER `is_final`,
  ADD COLUMN `allow_modification` TINYINT NOT NULL DEFAULT 1 COMMENT '允许修改:0否,1是' AFTER `allow_commercial_use`,
  ADD COLUMN `license_type` VARCHAR(50) DEFAULT 'Personal' COMMENT '授权类型:Personal个人/Commercial商业/Custom自定义' AFTER `allow_modification`;

-- 6. 为sys_user添加信誉分字段
ALTER TABLE `sys_user`
  ADD COLUMN `reputation_score` INT DEFAULT 80 COMMENT '信誉评分(设计者)' AFTER `avatar`;
