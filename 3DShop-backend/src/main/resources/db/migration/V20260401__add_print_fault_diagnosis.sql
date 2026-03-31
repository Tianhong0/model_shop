-- 打印故障类型配置表
CREATE TABLE IF NOT EXISTS `print_fault_type` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `fault_code` varchar(64) NOT NULL COMMENT '故障代码',
  `fault_category` varchar(32) NOT NULL COMMENT '故障分类（MODEL/PARAM/MATERIAL/DEVICE/UNKNOWN）',
  `fault_name` varchar(128) NOT NULL COMMENT '故障名称',
  `description` varchar(512) DEFAULT NULL COMMENT '故障描述',
  `suggestion` text COMMENT '处理建议（JSON数组格式）',
  `error_keywords` text COMMENT '错误关键词匹配（JSON数组格式）',
  `priority` int DEFAULT 0 COMMENT '匹配优先级（数值越大优先级越高）',
  `is_active` tinyint DEFAULT 1 COMMENT '是否启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fault_code` (`fault_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打印故障类型配置表';

-- 预置故障类型数据
INSERT INTO `print_fault_type` (`id`, `fault_code`, `fault_category`, `fault_name`, `description`, `suggestion`, `error_keywords`, `priority`) VALUES
(1, 'MODEL_FILE_NOT_FOUND', 'MODEL', '模型文件缺失', '模型文件不存在或已被删除', '["联系客服重新上传模型", "检查模型文件是否完整"]', '["模型源文件不存在", "file not found", "模型文件缺失", "source file"]', 100),
(2, 'MODEL_FORMAT_ERROR', 'MODEL', '模型格式错误', '模型文件格式不受支持或文件损坏', '["检查模型文件是否完整", "尝试重新导出模型文件", "联系客服寻求帮助"]', '["模型扩展名缺失", "invalid format", "corrupted", "解析失败", "unsupported format"]', 90),
(3, 'SLICE_TIMEOUT', 'PARAM', '切片超时', '切片处理时间过长，可能是模型过于复杂', '["简化模型结构", "降低模型精度", "联系客服调整参数"]', '["切片超时", "slice timeout", "timed out"]', 80),
(4, 'SLICE_FAILED', 'PARAM', '切片失败', '切片过程中发生错误', '["检查模型是否有非流形几何体", "检查模型法线方向", "调整打印参数后重试"]', '["切片失败", "slice failed", "slicing error", "cannot slice"]', 70),
(5, 'PRINTER_OFFLINE', 'DEVICE', '打印机离线', '打印机无法连接或已离线', '["检查打印机电源和网络连接", "等待打印机恢复在线", "联系客服报修"]', '["offline", "连接失败", "unreachable", "connection refused", "printer offline"]', 100),
(6, 'PRINTER_ERROR', 'DEVICE', '打印机故障', '打印机运行过程中发生错误', '["检查打印机状态", "清理打印平台", "联系客服报修"]', '["printer error", "硬件故障", "温度异常", "heating failed", "sensor error"]', 90),
(7, 'PRINT_FAILED', 'DEVICE', '打印失败', '打印过程中断或失败', '["清理打印平台", "检查耗材是否充足", "重新尝试打印"]', '["print failed", "printing error", "stopped", "aborted", "cancelled"]', 60),
(8, 'MATERIAL_ERROR', 'MATERIAL', '材料异常', '打印材料不足或质量问题', '["检查材料是否充足", "更换打印材料", "联系客服更换材料"]', '["材料不足", "filament error", "耗材", "out of filament", "material error"]', 50),
(9, 'UNKNOWN_ERROR', 'UNKNOWN', '未知错误', '发生未知类型的错误', '["尝试重新打印", "如问题持续请联系客服"]', '[]', 0);

-- 打印故障诊断记录表
CREATE TABLE IF NOT EXISTS `print_fault_diagnosis` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `job_id` bigint NOT NULL COMMENT '打印任务ID',
  `order_id` bigint DEFAULT NULL COMMENT '订单ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `fault_type_id` bigint DEFAULT NULL COMMENT '故障类型ID',
  `fault_code` varchar(64) DEFAULT NULL COMMENT '故障代码',
  `fault_category` varchar(32) DEFAULT NULL COMMENT '故障分类',
  `fault_name` varchar(128) DEFAULT NULL COMMENT '故障名称',
  `error_message` text COMMENT '原始错误信息',
  `analysis_result` text COMMENT '诊断分析结果（JSON）',
  `status` tinyint DEFAULT 0 COMMENT '处理状态（0-未处理 1-已重试 2-已联系客服 3-已解决）',
  `retry_count` int DEFAULT 0 COMMENT '重试次数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_job_id` (`job_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打印故障诊断记录表';
