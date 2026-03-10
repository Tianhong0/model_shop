package org.majun.backend.enums;

public enum UsedListingStatus {
    DRAFT(0, "草稿"),
    ON_SALE(1, "在售"),
    OFF_SHELF(2, "已下架"),
    SOLD(3, "已成交");

    private final int code;
    private final String description;

    UsedListingStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static UsedListingStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (UsedListingStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
