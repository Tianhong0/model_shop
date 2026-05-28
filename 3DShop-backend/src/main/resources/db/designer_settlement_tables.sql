-- =============================================
-- 设计者模型审核 + 分润结算 相关表
-- =============================================

-- 1. sys_model 表新增字段
ALTER TABLE `sys_model`
  ADD COLUMN `source_type`        TINYINT DEFAULT 1  COMMENT '来源: 1-OFFICIAL(官方), 2-DESIGNER(设计师上传)' AFTER `designer_id`,
  ADD COLUMN `profit_share_ratio` INT     DEFAULT 0  COMMENT '设计师分润比例(百分比，如30表示30%)' AFTER `base_price`,
  ADD COLUMN `audit_by`           BIGINT  DEFAULT NULL COMMENT '审核人ID(关联sys_user.id)' AFTER `status`,
  ADD COLUMN `audit_time`         DATETIME DEFAULT NULL COMMENT '审核时间' AFTER `audit_by`,
  ADD COLUMN `audit_note`         VARCHAR(500) DEFAULT NULL COMMENT '审核备注/驳回原因' AFTER `audit_time`;

-- 2. 模型审核记录表
CREATE TABLE IF NOT EXISTS `model_audit_record` (
    `id`                  BIGINT       NOT NULL COMMENT '主键ID',
    `model_id`            BIGINT       NOT NULL COMMENT '关联模型ID',
    `audit_by`            BIGINT       NOT NULL COMMENT '审核人ID',
    `action`              TINYINT      NOT NULL COMMENT '审核动作: 1-通过, 2-驳回',
    `profit_share_ratio`  INT          DEFAULT NULL COMMENT '通过时设置的分润比例',
    `note`                VARCHAR(500) DEFAULT NULL COMMENT '审核备注/驳回原因',
    `snapshot_data`       TEXT         DEFAULT NULL COMMENT '审核时的模型参数快照(JSON)',
    `is_delete`           TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    `create_time`         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_model_id` (`model_id`),
    KEY `idx_audit_by` (`audit_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型审核记录表';

-- 3. 设计师分润结算表
CREATE TABLE IF NOT EXISTS `designer_settlement` (
    `id`                  BIGINT        NOT NULL COMMENT '主键ID',
    `settlement_sn`       VARCHAR(64)   NOT NULL COMMENT '结算流水号(DS+时间戳+订单ID)',
    `order_id`            BIGINT        NOT NULL COMMENT '关联订单ID',
    `order_sn`            VARCHAR(64)   NOT NULL COMMENT '关联订单编号',
    `model_id`            BIGINT        NOT NULL COMMENT '关联模型ID',
    `designer_id`         BIGINT        NOT NULL COMMENT '设计师用户ID',
    `order_price`         DECIMAL(12,2) NOT NULL COMMENT '订单金额',
    `profit_share_ratio`  INT           NOT NULL COMMENT '当时的分润比例',
    `settlement_amount`   DECIMAL(12,2) NOT NULL COMMENT '结算金额(分润后)',
    `biz_type`            VARCHAR(32)   NOT NULL DEFAULT 'MODEL_PROFIT' COMMENT '业务类型',
    `status`              TINYINT       NOT NULL DEFAULT 0 COMMENT '结算状态: 0-待结算, 1-已结算, 2-结算失败',
    `wallet_ledger_id`    BIGINT        DEFAULT NULL COMMENT '关联钱包流水ID',
    `remark`              VARCHAR(500)  DEFAULT NULL COMMENT '备注',
    `is_delete`           TINYINT       DEFAULT 0 COMMENT '逻辑删除',
    `create_time`         DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_settlement_sn` (`settlement_sn`),
    UNIQUE KEY `uk_order_id` (`order_id`),
    KEY `idx_designer_id` (`designer_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设计师分润结算表';

-- 4. 兼容已建库：扩展 settlement_sn / order_sn 字段长度
--    旧版 VARCHAR(32) 无法容纳"DS+14位时间戳+19位雪花订单ID"
ALTER TABLE `designer_settlement`
  MODIFY COLUMN `settlement_sn` VARCHAR(64) NOT NULL COMMENT '结算流水号(DS+时间戳+订单ID)',
  MODIFY COLUMN `order_sn`      VARCHAR(64) NOT NULL COMMENT '关联订单编号';
