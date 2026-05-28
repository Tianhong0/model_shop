package org.majun.backend.enums;

import lombok.Getter;

/**
 * 模型来源类型枚举
 */
@Getter
public enum ModelSourceType {

    OFFICIAL(1, "官方"),
    DESIGNER(2, "设计者作品");

    private final int code;
    private final String description;

    ModelSourceType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static String descriptionOf(Integer code) {
        if (code == null) {
            return null;
        }
        for (ModelSourceType t : values()) {
            if (t.code == code) {
                return t.description;
            }
        }
        return null;
    }
}
