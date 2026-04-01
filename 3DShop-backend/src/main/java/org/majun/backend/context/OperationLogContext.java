package org.majun.backend.context;

import org.majun.backend.entity.SysOperationLog;

/**
 * 操作日志上下文
 * 用于在方法内部设置变更前数据
 */
public class OperationLogContext {

    private static final ThreadLocal<SysOperationLog> LOG_CONTEXT = new ThreadLocal<>();

    public static void setLog(SysOperationLog log) {
        LOG_CONTEXT.set(log);
    }

    public static SysOperationLog getLog() {
        return LOG_CONTEXT.get();
    }

    public static void clear() {
        LOG_CONTEXT.remove();
    }

    /**
     * 设置变更前数据
     */
    public static void setBeforeData(String beforeData) {
        SysOperationLog log = LOG_CONTEXT.get();
        if (log != null) {
            log.setBeforeData(beforeData);
        }
    }

    /**
     * 设置变更后数据
     */
    public static void setAfterData(String afterData) {
        SysOperationLog log = LOG_CONTEXT.get();
        if (log != null) {
            log.setAfterData(afterData);
        }
    }

    /**
     * 设置目标对象ID
     */
    public static void setTargetId(Long targetId) {
        SysOperationLog log = LOG_CONTEXT.get();
        if (log != null) {
            log.setTargetId(targetId);
        }
    }

    /**
     * 设置操作内容
     */
    public static void setContent(String content) {
        SysOperationLog log = LOG_CONTEXT.get();
        if (log != null) {
            log.setContent(content);
        }
    }
}
