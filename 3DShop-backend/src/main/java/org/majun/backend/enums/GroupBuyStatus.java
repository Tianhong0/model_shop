package org.majun.backend.enums;

/**
 * 拼团状态枚举
 */
public enum GroupBuyStatus {
    IN_PROGRESS(0, "拼团中"),
    SUCCESS(1, "拼团成功"),
    FAILED(2, "拼团失败"),
    CANCELED(3, "已取消");

    private final Integer code;
    private final String description;

    GroupBuyStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static GroupBuyStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (GroupBuyStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
