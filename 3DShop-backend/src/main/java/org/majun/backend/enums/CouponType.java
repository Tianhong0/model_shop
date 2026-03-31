package org.majun.backend.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 优惠券类型枚举
 */
@Getter
@AllArgsConstructor
public enum CouponType {

    FULL_REDUCTION(1, "满减券"),
    DISCOUNT(2, "折扣券"),
    CASH(3, "现金券");

    private final int code;
    private final String description;

    public static CouponType fromCode(int code) {
        for (CouponType type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        return null;
    }
}
