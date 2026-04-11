package org.majun.backend.enums;

/**
 * 钱包提现状态枚举
 */
public enum WalletWithdrawStatus {

    APPLIED(0, "待审核"),
    APPROVED(1, "审核通过待打款"),
    REJECTED(2, "已拒绝"),
    PAID(3, "已打款"),
    PAY_FAILED(4, "打款失败");

    private final int code;
    private final String description;

    WalletWithdrawStatus(int code, String description) {
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
