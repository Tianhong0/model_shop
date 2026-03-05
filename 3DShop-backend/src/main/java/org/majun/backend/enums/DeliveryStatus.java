package org.majun.backend.enums;

public enum DeliveryStatus {
    PENDING(0, "Pending shipment"),
    SHIPPED(1, "Shipped"),
    IN_TRANSIT(2, "In transit"),
    SIGNED(3, "Signed"),
    EXCEPTION(4, "Exception");

    private final int code;
    private final String description;

    DeliveryStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static DeliveryStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DeliveryStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
