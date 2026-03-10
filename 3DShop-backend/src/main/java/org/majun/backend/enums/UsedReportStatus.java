package org.majun.backend.enums;

public enum UsedReportStatus {
    PENDING(0, "待处理"),
    PROCESSING(1, "处理中"),
    RESOLVED(2, "已处理"),
    REJECTED(3, "已驳回");

    private final int code;
    private final String description;

    UsedReportStatus(int code, String description) {
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
