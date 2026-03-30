ALTER TABLE bounty_task ADD COLUMN cancel_requested INT DEFAULT 0 COMMENT '取消申请:0否1已申请';
