package org.majun.backend.enums;

public enum PointLedgerDirection {

    INCOME(1, "收入"),
    EXPENSE(2, "支出");

    private final int code;
    private final String description;

    PointLedgerDirection(int code, String description) {
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
