package org.majun.backend.enums;

public enum WalletLedgerDirection {

    INCOME(1, "收入"),
    EXPENSE(2, "支出"),
    FREEZE(3, "冻结"),
    UNFREEZE(4, "解冻");

    private final int code;
    private final String description;

    WalletLedgerDirection(int code, String description) {
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
