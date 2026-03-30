package org.majun.backend.enums;

public enum BountyTaskStatus {

    PENDING_REVIEW(-1, "待审核"),
    WAIT_ESCROW_PAYMENT(0, "待支付托管"),
    RECRUITING(1, "招募中"),
    PICKED(2, "已选标"),
    IN_DELIVERY(3, "交付中"),
    WAIT_ACCEPTANCE(4, "待验收"),
    COMPLETED(5, "已完成"),
    CLOSED(6, "已关闭"),
    DISPUTED(7, "争议中");

    private final int code;
    private final String description;

    BountyTaskStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static BountyTaskStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (BountyTaskStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
