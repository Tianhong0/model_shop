package org.majun.backend.ai.prompt;

import org.majun.backend.ai.model.IntentType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 提示词模板管理
 */
@Service
public class PromptTemplateManager {

    private static final String BASE_SYSTEM_PROMPT = """
            你是3D打印电商平台"3DShop"的智能客服助手。

            ## 你的职责
            1. 热情、专业地回答用户关于3D打印的问题
            2. 提供材料推荐和价格咨询
            3. 帮助用户了解订单状态和配送信息
            4. 对无法解决的问题，引导用户转人工客服

            ## 沟通规范
            - 使用简洁、友好的语言
            - 避免过于技术化的术语
            - 适当使用表情符号增加亲和力（如：😊、👍）
            - 回复长度控制在200字以内
            - 如果不确定答案，请诚实告知，不要编造信息
            - 用户要求转人工时，请回复"好的，正在为您转接人工客服..."

            ## 当前业务信息
            %s

            ## 对话上下文
            当前时间: %s
            """;

    /**
     * 各意图的额外提示
     */
    private static final Map<IntentType, String> INTENT_PROMPTS = Map.of(
            IntentType.PRINT_PRICE, """
                    用户询问的是打印价格问题。请重点：
                    1. 解释计价方式
                    2. 提供价格参考区间
                    3. 引导上传模型获取准确报价
                    """,

            IntentType.MATERIAL, """
                    用户询问的是材料问题。请重点：
                    1. 根据用户需求推荐合适的材料
                    2. 说明材料特性和适用场景
                    3. 提供价格对比
                    """,

            IntentType.DELIVERY, """
                    用户询问的是配送问题。请重点：
                    1. 说明打印周期
                    2. 解释物流时间
                    3. 如果有订单号，可以帮助查询物流
                    """,

            IntentType.ORDER_STATUS, """
                    用户询问的是订单问题。请重点：
                    1. 引导用户提供订单号或在"我的订单"查看
                    2. 解释订单状态含义
                    3. 如有异常，引导联系人工客服
                    """,

            IntentType.COMPLAINT, """
                    用户可能是投诉或不满。请重点：
                    1. 表示理解和歉意
                    2. 了解具体问题
                    3. 提供解决方案或主动转人工客服
                    """,

            IntentType.TECHNICAL, """
                    用户询问的是技术问题。请重点：
                    1. 了解具体的打印需求
                    2. 提供专业的技术建议
                    3. 如果问题复杂，建议上传模型或联系技术支持
                    """
    );

    /**
     * 构建系统提示词
     *
     * @param intent          意图类型
     * @param businessContext 业务上下文
     * @return 完整的系统提示词
     */
    public String buildSystemPrompt(IntentType intent, String businessContext) {
        String intentPrompt = INTENT_PROMPTS.getOrDefault(intent, "");
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        return String.format(BASE_SYSTEM_PROMPT,
                businessContext + "\n" + intentPrompt,
                currentTime);
    }

    /**
     * 获取意图识别提示词
     */
    public String getIntentClassificationPrompt(String userMessage) {
        return """
                你是一个客服意图分类专家。请分析用户的问题，判断其意图类别。

                可选类别：
                - PRINT_PRICE: 打印价格咨询（询问3D打印费用、报价）
                - MATERIAL: 材料推荐（询问材料选择、材料特性）
                - DELIVERY: 配送时间（询问发货时间、物流状态）
                - ORDER_STATUS: 订单状态（查询订单进度、支付状态）
                - TECHNICAL: 技术问题（打印参数、文件格式、建模问题）
                - COMPLAINT: 投诉建议（服务投诉、售后问题）
                - OTHER: 其他问题

                请只返回JSON格式，不要包含其他内容：
                {"intent": "类别", "confidence": 0.0-1.0, "reason": "简短原因"}

                用户问题：%s
                """.formatted(userMessage);
    }

    /**
     * 获取会话摘要提示词
     */
    public String getSummaryPrompt(String conversationText) {
        return """
                请总结以下客服对话的关键信息，包括：
                1. 用户的主要问题
                2. 已提供的解决方案
                3. 未解决的问题
                4. 建议的处理方式

                对话内容：
                %s

                请用简洁的中文回复（不超过200字）。
                """.formatted(conversationText);
    }

    /**
     * 获取意图识别系统提示词
     */
    public String getIntentSystemPrompt() {
        return "你是一个意图分类专家，只返回JSON格式结果，不要包含其他内容。";
    }

    /**
     * 获取摘要系统提示词
     */
    public String getSummarySystemPrompt() {
        return "你是一个客服对话摘要专家，用简洁的中文总结对话内容。";
    }
}
