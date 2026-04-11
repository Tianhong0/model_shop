package org.majun.backend.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据范围类型枚举
 */
@Getter
@AllArgsConstructor
/**
 * 数据范围类型枚举
 */
public enum DataScopeType {

    /**
     * 全部数据权限
     */
    ALL(1, "全部数据"),

    /**
     * 仅本人数据权限
     */
    SELF(5, "仅本人数据");

    private final Integer code;
    private final String description;

    /**
     * 根据code获取枚举
     */
    public static DataScopeType fromCode(Integer code) {
        if (code == null) {
            return SELF;
        }
        for (DataScopeType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return SELF;
    }

    /**
     * 判断是否为全部数据权限
     */
    public boolean isAll() {
        return this == ALL;
    }

    /**
     * 判断是否为仅本人数据权限
     */
    public boolean isSelf() {
        return this == SELF;
    }
}
