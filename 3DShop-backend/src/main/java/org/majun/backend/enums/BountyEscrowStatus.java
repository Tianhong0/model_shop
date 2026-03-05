package org.majun.backend.enums;

public enum BountyEscrowStatus {

    WAIT_PAY(0, "待支付"),
    ESCROWED(1, "已托管"),
    PARTIAL_RELEASED(2, "部分释放"),
    RELEASED(3, "全部释放"),
    REFUNDED(4, "已退款"),
    CLOSED(5, "已关闭");

    private final int code;
    private final String description;

    BountyEscrowStatus(int code, String description) {
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
