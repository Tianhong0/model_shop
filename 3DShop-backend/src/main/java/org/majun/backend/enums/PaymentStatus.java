package org.majun.backend.enums;

/**
 * 支付状态枚举
 */
public enum PaymentStatus {
    PENDING(0, "Pending"),
    SUCCESS(1, "Success"),
    CLOSED(2, "Closed"),
    FAILED(3, "Failed");

    private final int code;
    private final String description;

    PaymentStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PaymentStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (PaymentStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
