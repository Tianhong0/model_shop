package org.majun.backend.enums;

/**
 * 打印机状态枚举
 */
public enum PrintPrinterStatus {
    IDLE(0, "Idle"),
    BUSY(1, "Busy"),
    OFFLINE(2, "Offline"),
    ERROR(3, "Error");

    private final int code;
    private final String description;

    PrintPrinterStatus(int code, String description) {
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
