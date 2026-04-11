package org.majun.backend.enums;

/**
 * 账户删除状态枚举
 */
public enum DeletionStatus {
    PENDING("pending", "待审批"),
    APPROVED("approved", "已批准"),
    REJECTED("rejected", "已拒绝");

    private final String code;
    private final String description;

    DeletionStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}