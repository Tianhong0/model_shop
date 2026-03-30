-- 环保材料推广功能 - 数据库迁移脚本
-- 为 sys_model_material 表添加环保标识字段

ALTER TABLE `sys_model_material`
ADD COLUMN `is_eco` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否环保材质: 1-是, 0-否' AFTER `is_trusted`;

-- 添加索引以优化环保材质优先排序查询
ALTER TABLE `sys_model_material`
ADD INDEX `idx_model_eco` (`model_id`, `is_eco` DESC);

-- 示例：将某些材质标记为环保材质（根据实际业务需求修改）
-- UPDATE `sys_model_material` SET `is_eco` = 1 WHERE `material_name` LIKE '%PLA%' OR `material_name` LIKE '%环保%';
