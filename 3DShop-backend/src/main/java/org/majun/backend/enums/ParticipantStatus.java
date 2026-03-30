package org.majun.backend.enums;

/**
 * 拼团参与状态枚举
 */
public enum ParticipantStatus {
    PENDING_PAYMENT(0, "待支付"),
    PAID(1, "已支付"),
    CANCELED(2, "已取消"),
    REFUNDED(3, "已退款");

    private final Integer code;
    private final String description;

    ParticipantStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ParticipantStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ParticipantStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
