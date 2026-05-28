-- 邀请落地页相关配置
-- 执行顺序：在 promotion_tables.sql 之后执行

-- 扩展 config_value 字段长度，支持长 URL
ALTER TABLE `promotion_config` MODIFY COLUMN `config_value` TEXT NOT NULL COMMENT '配置值';

INSERT INTO `promotion_config` (`id`, `config_key`, `config_value`, `config_desc`, `status`) VALUES
(7, 'APP_DOWNLOAD_URL', '', 'APP下载地址（用于邀请落地页未安装APP时的下载跳转）', 1),
(8, 'APP_URL_SCHEME', 'threedshop', 'APP的URL Scheme（用于浏览器唤起APP）', 1)
ON DUPLICATE KEY UPDATE `id` = VALUES(`id`);
