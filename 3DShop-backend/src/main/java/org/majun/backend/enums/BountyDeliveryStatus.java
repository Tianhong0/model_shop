package org.majun.backend.enums;

public enum BountyDeliveryStatus {

    WAIT_SUBMIT(0, "待提交"),
    SUBMITTED(1, "已提交待确认"),
    NEED_REWORK(2, "已退回修改"),
    ACCEPTED(3, "已验收通过"),
    REJECTED(4, "已拒绝");

    private final int code;
    private final String description;

    BountyDeliveryStatus(int code, String description) {
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
