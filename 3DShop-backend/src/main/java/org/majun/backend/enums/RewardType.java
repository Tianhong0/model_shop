package org.majun.backend.enums;

/**
 * 推广奖励类型枚举
 */
public enum RewardType {

    INVITE_REGISTER("INVITE_REGISTER", "邀请注册奖励"),
    FIRST_ORDER("FIRST_ORDER", "首单奖励"),
    CONSUME_REBATE("CONSUME_REBATE", "消费返积分");

    private final String code;
    private final String description;

    RewardType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static RewardType fromCode(String code) {
        for (RewardType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
