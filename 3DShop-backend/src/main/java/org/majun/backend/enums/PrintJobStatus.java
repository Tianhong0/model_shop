package org.majun.backend.enums;

/**
 * 打印任务状态枚举
 */
public enum PrintJobStatus {
    QUEUED(0, "排队中"),
    SLICING(1, "切片中"),
    SLICE_FAILED(2, "切片失败"),
    READY_TO_PRINT(3, "待打印"),
    PRINTING(4, "打印中"),
    PAUSED(5, "已暂停"),
    DONE(6, "已完成"),
    FAILED(7, "打印失败"),
    CANCELED(8, "已取消");

    private final int code;
    private final String description;

    PrintJobStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PrintJobStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (PrintJobStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
