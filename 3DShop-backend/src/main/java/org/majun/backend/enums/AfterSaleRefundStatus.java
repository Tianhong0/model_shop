package org.majun.backend.enums;

/**
 * 售后退款状态枚举
 */
public enum AfterSaleRefundStatus {
    NONE(0, "None"),
    PENDING(1, "Pending"),
    SUCCESS(2, "Success"),
    FAILED(3, "Failed");

    private final int code;
    private final String description;

    AfterSaleRefundStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static AfterSaleRefundStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AfterSaleRefundStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
