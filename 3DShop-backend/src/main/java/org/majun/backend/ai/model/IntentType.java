package org.majun.backend.ai.model;

import lombok.Getter;

/**
 * 用户意图类型枚举
 */
@Getter
public enum IntentType {

    PRINT_PRICE("打印价格咨询", "询问3D打印费用、报价"),
    MATERIAL("材料推荐", "询问材料选择、材料特性"),
    DELIVERY("配送时间", "询问发货时间、物流状态"),
    ORDER_STATUS("订单状态", "查询订单进度、支付状态"),
    TECHNICAL("技术问题", "打印参数、文件格式、建模问题"),
    COMPLAINT("投诉建议", "服务投诉、售后问题"),
    OTHER("其他问题", "无法分类的问题");

    private final String name;
    private final String description;

    IntentType(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
