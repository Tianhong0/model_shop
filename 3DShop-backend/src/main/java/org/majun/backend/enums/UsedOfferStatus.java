package org.majun.backend.enums;

public enum UsedOfferStatus {
    PENDING(0, "待处理"),
    ACCEPTED(1, "已接受"),
    REJECTED(2, "已拒绝"),
    CANCELED(3, "已撤回"),
    EXPIRED(4, "已过期");

    private final int code;
    private final String description;

    UsedOfferStatus(int code, String description) {
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
