package org.majun.backend.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.ai.model.AiChatRequest;
import org.majun.backend.ai.model.AiChatResponse;
import org.majun.backend.ai.model.ContentModerationResult;
import org.majun.backend.ai.provider.AiModelProvider;
import org.majun.backend.config.AiProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 内容审核服务
 * 使用AI检测并过滤敏感内容
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentModerationService {

    private final AiModelProvider aiModelProvider;
    private final AiProperties aiProperties;

    private static final String MODERATION_SYSTEM_PROMPT = """
        你是一个专业的内容审核助手。你的任务是检测用户提交的文本中是否包含敏感内容。

        敏感内容包括但不限于：
        1. 暴力恐怖相关词汇
        2. 色情低俗内容
        3. 政治敏感内容
        4. 违法犯罪相关
        5. 广告垃圾信息
        6. 人身攻击、辱骂、歧视性言论
        7. 个人隐私信息（手机号、身份证号、银行卡号等）

        请仔细分析用户提交的文本，按以下JSON格式返回结果（只返回JSON，不要其他解释）：
        {
            "hasSensitive": true或false,
            "sensitiveWords": ["敏感词1", "敏感词2"],
            "categories": ["类别1", "类别2"],
            "processedText": "将敏感词替换为*后的文本"
        }

        规则：
        1. 如果没有敏感内容，hasSensitive为false，sensitiveWords和categories为空数组，processedText为原文
        2. 如果有敏感内容，必须准确识别出所有敏感词
        3. processedText中将每个敏感词的每个字符都替换为*（如"敏感词"变为"***"）
        4. categories可选值：暴力、色情、政治、违法、广告、辱骂、隐私、其他
        """;

    /**
     * 审核文本内容
     *
     * @param text 待审核的文本
     * @return 审核结果
     */
    public ContentModerationResult moderate(String text) {
        if (!StringUtils.hasText(text)) {
            return ContentModerationResult.safe(text);
        }

        // 检查AI服务是否可用
        if (!aiModelProvider.isAvailable()) {
            log.warn("AI服务不可用，跳过内容审核");
            return ContentModerationResult.safe(text);
        }

        // 检查是否启用内容审核
        if (!isModerationEnabled()) {
            log.debug("内容审核未启用，跳过审核");
            return ContentModerationResult.safe(text);
        }

        try {
            AiChatRequest request = AiChatRequest.builder()
                    .systemPrompt(MODERATION_SYSTEM_PROMPT)
                    .userMessage(text)
                    .maxTokens(getMaxTokens())
                    .temperature(0.1)  // 低温度以获得更稳定的结果
                    .build();

            AiChatResponse response = aiModelProvider.chat(request);

            if (!response.getSuccess()) {
                log.error("AI审核调用失败: {}", response.getErrorMessage());
                return ContentModerationResult.failed(response.getErrorMessage());
            }

            return parseModerationResponse(response.getContent(), text);
        } catch (Exception e) {
            log.error("内容审核异常", e);
            return ContentModerationResult.failed("内容审核服务异常: " + e.getMessage());
        }
    }

    /**
     * 审核文本内容，如果失败则返回原文
     *
     * @param text 待审核的文本
     * @return 处理后的文本（敏感词已替换为*）
     */
    public String moderateText(String text) {
        ContentModerationResult result = moderate(text);
        if (result.getSuccess() && result.getHasSensitiveContent()) {
            log.info("检测到敏感内容，已过滤。敏感词: {}, 类别: {}",
                    result.getSensitiveWords(), result.getCategories());
        }
        return result.getSuccess() ? result.getProcessedText() : text;
    }

    /**
     * 批量审核多个文本字段
     *
     * @param texts 待审核的文本数组
     * @return 处理后的文本数组
     */
    public String[] moderateTexts(String... texts) {
        if (texts == null || texts.length == 0) {
            return texts;
        }
        String[] results = new String[texts.length];
        for (int i = 0; i < texts.length; i++) {
            results[i] = moderateText(texts[i]);
        }
        return results;
    }

    /**
     * 解析AI返回的审核结果
     */
    private ContentModerationResult parseModerationResponse(String aiResponse, String originalText) {
        if (!StringUtils.hasText(aiResponse)) {
            return ContentModerationResult.safe(originalText);
        }

        try {
            // 提取JSON部分（处理AI可能返回的额外文本）
            String jsonStr = extractJson(aiResponse);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(jsonStr);

            boolean hasSensitive = node.path("hasSensitive").asBoolean(false);
            List<String> sensitiveWords = parseStringList(node.path("sensitiveWords"));
            List<String> categories = parseStringList(node.path("categories"));
            String processedText = node.path("processedText").asText(originalText);

            // 二次验证：确保敏感词确实被替换
            if (hasSensitive && !sensitiveWords.isEmpty()) {
                processedText = ensureSensitiveWordsReplaced(originalText, sensitiveWords);
            }

            return ContentModerationResult.builder()
                    .hasSensitiveContent(hasSensitive)
                    .processedText(processedText)
                    .sensitiveWords(sensitiveWords)
                    .categories(categories)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("解析审核结果失败: {}", e.getMessage());
            // 解析失败时，尝试使用正则提取敏感词并替换
            return fallbackModeration(originalText, aiResponse);
        }
    }

    /**
     * 从AI响应中提取JSON
     */
    private String extractJson(String response) {
        // 尝试找到JSON对象
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    /**
     * 解析字符串列表
     */
    private List<String> parseStringList(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                result.add(item.asText());
            }
        }
        return result;
    }

    /**
     * 确保敏感词被正确替换为*
     */
    private String ensureSensitiveWordsReplaced(String originalText, List<String> sensitiveWords) {
        String result = originalText;
        for (String word : sensitiveWords) {
            if (StringUtils.hasText(word)) {
                String stars = "*".repeat(word.length());
                result = result.replace(word, stars);
            }
        }
        return result;
    }

    /**
     * 降级审核方案：当JSON解析失败时使用
     */
    private ContentModerationResult fallbackModeration(String originalText, String aiResponse) {
        // 尝试从AI响应中提取可能的敏感词
        List<String> possibleWords = new ArrayList<>();
        Pattern pattern = Pattern.compile("[\"']([^\"']+)[\"']");
        Matcher matcher = pattern.matcher(aiResponse);
        while (matcher.find()) {
            String word = matcher.group(1);
            if (word.length() >= 2 && originalText.contains(word)) {
                possibleWords.add(word);
            }
        }

        if (possibleWords.isEmpty()) {
            return ContentModerationResult.safe(originalText);
        }

        String processedText = ensureSensitiveWordsReplaced(originalText, possibleWords);
        return ContentModerationResult.builder()
                .hasSensitiveContent(true)
                .processedText(processedText)
                .sensitiveWords(possibleWords)
                .categories(List.of("其他"))
                .success(true)
                .build();
    }

    /**
     * 检查是否启用内容审核
     */
    private boolean isModerationEnabled() {
        return aiProperties.getEnabled() != null && aiProperties.getEnabled();
    }

    /**
     * 获取最大Token数
     */
    private Integer getMaxTokens() {
        // 内容审核不需要太多token，使用默认值或配置值
        return 1024;
    }
}
