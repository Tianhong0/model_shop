-- 推广配置更新 - 添加海报相关配置
-- 执行顺序：在 promotion_tables.sql 之后

-- 添加海报相关配置
INSERT INTO `promotion_config` (`id`, `config_key`, `config_value`, `config_desc`, `status`) VALUES
(10, 'POSTER_TITLE', '印力无限', '海报标题', 1),
(11, 'POSTER_SUBTITLE', '邀请好友注册，双方均可获得积分奖励', '海报副标题', 1),
(12, 'POSTER_BG_COLOR_START', '#00bfff', '海报背景渐变起始色', 1),
(13, 'POSTER_BG_COLOR_END', '#0099cc', '海报背景渐变结束色', 1),
(14, 'POSTER_BG_IMAGE', '', '海报背景图片URL（为空则使用渐变色）', 1),
(15, 'POSTER_TITLE_COLOR', '#1a2030', '海报标题文字颜色', 1),
(16, 'POSTER_CODE_COLOR', '#00bfff', '邀请码文字颜色', 1),
(17, 'POSTER_TIPS_TEXT', '长按保存图片，分享给好友', '海报底部提示文字', 1),
(18, 'POSTER_WIDTH', '200', '海报宽度（像素）', 1),
(19, 'POSTER_HEIGHT', '280', '海报高度（像素）', 1),
(20, 'POSTER_QRCODE_SIZE', '70', '二维码尺寸（像素）', 1)
ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`);
