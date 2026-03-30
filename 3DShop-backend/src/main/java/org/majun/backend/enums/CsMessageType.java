package org.majun.backend.enums;

public enum CsMessageType {

    TEXT(1, "文本"),
    IMAGE(2, "图片"),
    VIDEO(3, "视频"),
    SYSTEM(4, "系统消息");

    private final int code;
    private final String description;

    CsMessageType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static CsMessageType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (CsMessageType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
