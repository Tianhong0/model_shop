package org.majun.backend.enums;

/**
 * 售后状态枚举
 */
public enum AfterSaleStatus {
    APPLIED(0, "Applied"),
    REVIEWING(1, "Reviewing"),
    PROCESSING(2, "Processing"),
    REFUNDING(3, "Refunding"),
    COMPLETED(4, "Completed"),
    REJECTED(5, "Rejected"),
    CANCELED(6, "Canceled");

    private final int code;
    private final String description;

    AfterSaleStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static AfterSaleStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AfterSaleStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
