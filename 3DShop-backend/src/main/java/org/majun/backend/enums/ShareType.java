package org.majun.backend.enums;

/**
 * 分享类型枚举
 */
public enum ShareType {

    MODEL("MODEL", "商品分享"),
    POSTER("POSTER", "海报分享"),
    LINK("LINK", "链接分享");

    private final String code;
    private final String description;

    ShareType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ShareType fromCode(String code) {
        for (ShareType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
