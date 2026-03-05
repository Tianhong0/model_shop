package org.majun.backend.enums;

public enum OrderStatus {
    PENDING_PAYMENT(0, "Pending payment"),
    IN_PRODUCTION(1, "In production"),
    WAIT_SHIPMENT(2, "Waiting shipment"),
    COMPLETED(3, "Completed"),
    CANCELED(4, "Canceled");

    private final int code;
    private final String description;

    OrderStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static OrderStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (OrderStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
