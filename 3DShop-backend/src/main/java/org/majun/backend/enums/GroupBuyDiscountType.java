package org.majun.backend.enums;

/**
 * 拼团折扣类型枚举
 */
/**
 * 拼团折扣类型枚举
 */
public enum GroupBuyDiscountType {
    FIXED(1, "固定折扣"),
    LADDER(2, "阶梯折扣");

    private final Integer code;
    private final String description;

    GroupBuyDiscountType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static GroupBuyDiscountType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (GroupBuyDiscountType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
