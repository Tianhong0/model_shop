package org.majun.backend.enums;

/**
 * 二手订单状态枚举
 */
public enum UsedOrderStatus {
    PENDING_PAYMENT(0, "待支付"),
    WAIT_SHIPMENT(1, "待发货"),
    WAIT_RECEIVE(2, "待收货"),
    COMPLETED(3, "已完成"),
    CANCELED(4, "已取消"),
    AFTER_SALE(5, "售后中");

    private final int code;
    private final String description;

    UsedOrderStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
