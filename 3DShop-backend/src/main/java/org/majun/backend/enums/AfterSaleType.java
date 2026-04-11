package org.majun.backend.enums;

/**
 * 售后类型枚举
 */
public enum AfterSaleType {
    REFUND_ONLY(1, "Refund only"),
    RETURN_REFUND(2, "Return and refund"),
    REPRINT(3, "Reprint"),
    EXCHANGE(4, "Exchange");

    private final int code;
    private final String description;

    AfterSaleType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static AfterSaleType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AfterSaleType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
