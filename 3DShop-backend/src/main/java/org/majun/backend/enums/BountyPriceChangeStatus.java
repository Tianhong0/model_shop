package org.majun.backend.enums;

/**
 * 悬赏改价状态枚举
 */
public enum BountyPriceChangeStatus {

    PENDING(0, "待确认"),
    AGREED(1, "已同意"),
    REJECTED(2, "已拒绝"),
    EXPIRED(3, "已失效");

    private final int code;
    private final String description;

    BountyPriceChangeStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
