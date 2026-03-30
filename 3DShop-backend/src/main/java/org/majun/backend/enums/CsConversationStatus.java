package org.majun.backend.enums;

public enum CsConversationStatus {

    WAITING(0, "等待分配"),
    ACTIVE(1, "进行中"),
    ENDED(2, "已结束");

    private final int code;
    private final String description;

    CsConversationStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static CsConversationStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (CsConversationStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
