package org.majun.backend.common;

/**
 * 返回状态码工具类
 */
public class ResultUtil {

    /**
     * 获取状态码
     */
    public static Integer getCode(String code) {
        return 200;
    }

    /**
     * 获取成功消息
     */
    public static String getSuccessMessage() {
        return "操作成功";
    }
}
