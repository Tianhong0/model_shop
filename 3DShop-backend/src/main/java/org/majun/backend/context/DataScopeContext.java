package org.majun.backend.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.majun.backend.enums.DataScopeType;

/**
 * 数据权限上下文
 * 使用ThreadLocal存储当前请求的数据权限信息
 */
public class DataScopeContext {

    private static final ThreadLocal<DataScopeInfo> CONTEXT = new ThreadLocal<>();

    /**
     * 设置数据权限信息
     */
    public static void set(DataScopeInfo info) {
        CONTEXT.set(info);
    }

    /**
     * 获取数据权限信息
     */
    public static DataScopeInfo get() {
        return CONTEXT.get();
    }

    /**
     * 清除上下文
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        DataScopeInfo info = get();
        return info != null ? info.getUserId() : null;
    }

    /**
     * 获取当前数据范围类型
     */
    public static DataScopeType getDataScopeType() {
        DataScopeInfo info = get();
        return info != null ? info.getScopeType() : DataScopeType.SELF;
    }

    /**
     * 是否跳过数据权限检查（管理员）
     */
    public static boolean isSkipCheck() {
        DataScopeInfo info = get();
        return info != null && info.isSkipCheck();
    }

    /**
     * 设置跳过检查标记
     */
    public static void setSkipCheck(boolean skipCheck) {
        DataScopeInfo info = get();
        if (info != null) {
            info.setSkipCheck(skipCheck);
        }
    }

    /**
     * 数据权限信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataScopeInfo {

        /**
         * 当前用户ID
         */
        private Long userId;

        /**
         * 数据范围类型
         */
        private DataScopeType scopeType;

        /**
         * 是否跳过检查
         */
        private boolean skipCheck;

        /**
         * 资源类型
         */
        private String resourceType;

        /**
         * 归属字段名
         */
        private String ownerField;

        /**
         * 表别名
         */
        private String tableAlias;
    }
}
