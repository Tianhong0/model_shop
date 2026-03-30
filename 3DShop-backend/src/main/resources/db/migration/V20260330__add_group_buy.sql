-- 拼团功能相关表结构
-- V20260330__add_group_buy.sql

-- 拼团活动表
CREATE TABLE IF NOT EXISTS sys_group_buy_activity (
    id BIGINT NOT NULL COMMENT '活动ID',
    activity_name VARCHAR(100) NOT NULL COMMENT '活动名称',
    model_id BIGINT NOT NULL COMMENT '关联模型ID',
    min_people INT NOT NULL DEFAULT 2 COMMENT '最小拼团人数',
    max_people INT DEFAULT NULL COMMENT '最大拼团人数（NULL表示无限制）',
    discount_type TINYINT NOT NULL DEFAULT 1 COMMENT '折扣类型：1-固定折扣，2-阶梯折扣',
    discount_value DECIMAL(5,2) NOT NULL COMMENT '折扣值（固定折扣为百分比，如90表示9折）',
    ladder_config JSON DEFAULT NULL COMMENT '阶梯折扣配置JSON',
    original_price DECIMAL(10,2) NOT NULL COMMENT '原价',
    group_price DECIMAL(10,2) NOT NULL COMMENT '拼团价',
    start_time DATETIME NOT NULL COMMENT '活动开始时间',
    end_time DATETIME NOT NULL COMMENT '活动结束时间',
    timeout_hours INT NOT NULL DEFAULT 24 COMMENT '拼团超时时间（小时）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用，2-已结束',
    total_stock INT DEFAULT NULL COMMENT '总库存（NULL表示无限制）',
    sold_count INT NOT NULL DEFAULT 0 COMMENT '已售数量',
    cover_image VARCHAR(500) DEFAULT NULL COMMENT '活动封面图',
    description TEXT COMMENT '活动描述',
    is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_model_id (model_id),
    INDEX idx_status_time (status, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团活动表';

-- 拼团组表
CREATE TABLE IF NOT EXISTS sys_group_buy_group (
    id BIGINT NOT NULL COMMENT '拼团组ID',
    activity_id BIGINT NOT NULL COMMENT '活动ID',
    leader_user_id BIGINT NOT NULL COMMENT '团长用户ID',
    group_sn VARCHAR(50) NOT NULL COMMENT '拼团编号',
    current_people INT NOT NULL DEFAULT 1 COMMENT '当前人数',
    target_people INT NOT NULL COMMENT '目标人数',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-拼团中，1-拼团成功，2-拼团失败，3-已取消',
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '拼团总金额',
    expire_time DATETIME NOT NULL COMMENT '过期时间',
    success_time DATETIME DEFAULT NULL COMMENT '成功时间',
    fail_time DATETIME DEFAULT NULL COMMENT '失败时间',
    fail_reason VARCHAR(200) DEFAULT NULL COMMENT '失败原因',
    share_code VARCHAR(20) DEFAULT NULL COMMENT '分享码',
    is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_sn (group_sn),
    UNIQUE KEY uk_share_code (share_code),
    INDEX idx_activity_id (activity_id),
    INDEX idx_leader_user_id (leader_user_id),
    INDEX idx_status (status),
    INDEX idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团组表';

-- 拼团参与表
CREATE TABLE IF NOT EXISTS sys_group_buy_participant (
    id BIGINT NOT NULL COMMENT '参与ID',
    group_id BIGINT NOT NULL COMMENT '拼团ID',
    activity_id BIGINT NOT NULL COMMENT '活动ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_id BIGINT DEFAULT NULL COMMENT '关联订单ID',
    order_sn VARCHAR(50) DEFAULT NULL COMMENT '订单号',
    is_leader TINYINT NOT NULL DEFAULT 0 COMMENT '是否团长：0-否，1-是',
    quantity INT NOT NULL DEFAULT 1 COMMENT '购买数量',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '单价',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '总金额',
    material_id BIGINT DEFAULT NULL COMMENT '材质ID',
    color VARCHAR(50) DEFAULT NULL COMMENT '颜色',
    scale DECIMAL(5,2) DEFAULT 1.00 COMMENT '缩放比例',
    fill_percent DECIMAL(5,2) DEFAULT 100.00 COMMENT '填充密度',
    custom_params JSON DEFAULT NULL COMMENT '其他定制参数',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待支付，1-已支付，2-已取消，3-已退款',
    pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
    is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_group_id (group_id),
    INDEX idx_activity_id (activity_id),
    INDEX idx_user_id (user_id),
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团参与表';

-- 批量打印折扣配置表
CREATE TABLE IF NOT EXISTS sys_batch_print_discount (
    id BIGINT NOT NULL COMMENT '配置ID',
    min_quantity INT NOT NULL COMMENT '最小数量',
    max_quantity INT DEFAULT NULL COMMENT '最大数量（NULL表示无上限）',
    discount_percent DECIMAL(5,2) NOT NULL COMMENT '折扣百分比',
    description VARCHAR(100) DEFAULT NULL COMMENT '描述',
    is_active TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_quantity (min_quantity, max_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批量打印折扣配置表';

-- 初始化批量打印折扣配置
INSERT INTO sys_batch_print_discount (id, min_quantity, max_quantity, discount_percent, description, is_active, sort_order) VALUES
(1, 2, 4, 95.00, '2-4件 95折', 1, 1),
(2, 5, 9, 90.00, '5-9件 9折', 1, 2),
(3, 10, 19, 85.00, '10-19件 85折', 1, 3),
(4, 20, NULL, 80.00, '20件以上 8折', 1, 4);

-- 修改订单表，添加拼团相关字段
ALTER TABLE sys_order ADD COLUMN group_buy_group_id BIGINT DEFAULT NULL COMMENT '拼团组ID' AFTER printer_id;
ALTER TABLE sys_order ADD COLUMN group_buy_participant_id BIGINT DEFAULT NULL COMMENT '拼团参与ID' AFTER group_buy_group_id;
ALTER TABLE sys_order ADD COLUMN batch_quantity INT NOT NULL DEFAULT 1 COMMENT '批量打印数量' AFTER group_buy_participant_id;
ALTER TABLE sys_order ADD COLUMN batch_discount DECIMAL(5,2) DEFAULT NULL COMMENT '批量打印折扣' AFTER batch_quantity;

-- 添加索引
CREATE INDEX idx_order_group_buy ON sys_order (group_buy_group_id);
