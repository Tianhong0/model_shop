package org.majun.backend.enums;

public enum BountyBidStatus {

    SUBMITTED(0, "已提交"),
    WINNER(1, "已中标"),
    LOST(2, "未中标"),
    WITHDRAWN(3, "已撤回");

    private final int code;
    private final String description;

    BountyBidStatus(int code, String description) {
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
