package org.majun.backend.enums;

public enum UsedAfterSaleStatus {
    APPLIED(0, "待处理"),
    APPROVED(1, "已同意"),
    REJECTED(2, "已拒绝"),
    PLATFORM_INTERVENTION(3, "平台介入"),
    REFUNDED(4, "已退款"),
    CLOSED(5, "已关闭");

    private final int code;
    private final String description;

    UsedAfterSaleStatus(int code, String description) {
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
