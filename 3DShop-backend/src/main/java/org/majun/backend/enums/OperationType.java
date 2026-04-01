package org.majun.backend.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 操作类型枚举
 */
@Getter
@AllArgsConstructor
public enum OperationType {

    // 通用操作
    CREATE("CREATE", "新增"),
    UPDATE("UPDATE", "修改"),
    DELETE("DELETE", "删除"),
    QUERY("QUERY", "查询"),
    OTHER("OTHER", "其他"),

    // 认证相关
    LOGIN("LOGIN", "登录"),
    LOGOUT("LOGOUT", "登出"),
    REGISTER("REGISTER", "注册"),

    // 审核相关
    REVIEW("REVIEW", "审核"),
    APPROVE("APPROVE", "审批"),
    REJECT("REJECT", "驳回"),

    // 状态变更
    ENABLE("ENABLE", "启用"),
    DISABLE("DISABLE", "禁用"),
    STATUS_CHANGE("STATUS_CHANGE", "状态变更"),

    // 财务相关
    PAY("PAY", "支付"),
    REFUND("REFUND", "退款"),
    WITHDRAW("WITHDRAW", "提现"),

    // 物流相关
    SHIP("SHIP", "发货"),
    SIGN("SIGN", "签收"),

    // 其他
    EXPORT("EXPORT", "导出"),
    IMPORT("IMPORT", "导入");

    private final String code;
    private final String desc;

    public static OperationType fromCode(String code) {
        for (OperationType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return OTHER;
    }
}
