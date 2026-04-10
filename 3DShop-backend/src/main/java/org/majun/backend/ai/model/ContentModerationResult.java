package org.majun.backend.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 内容审核结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentModerationResult {

    /**
     * 是否包含敏感内容
     */
    private Boolean hasSensitiveContent;

    /**
     * 处理后的文本（敏感词已替换为*）
     */
    private String processedText;

    /**
     * 检测到的敏感词列表
     */
    private List<String> sensitiveWords;

    /**
     * 敏感词类别（如：暴力、色情、政治、广告等）
     */
    private List<String> categories;

    /**
     * 审核是否成功
     */
    private Boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建一个安全的结果（无敏感内容）
     */
    public static ContentModerationResult safe(String originalText) {
        return ContentModerationResult.builder()
                .hasSensitiveContent(false)
                .processedText(originalText)
                .sensitiveWords(List.of())
                .categories(List.of())
                .success(true)
                .build();
    }

    /**
     * 创建一个审核失败的结果
     */
    public static ContentModerationResult failed(String errorMessage) {
        return ContentModerationResult.builder()
                .hasSensitiveContent(false)
                .processedText(null)
                .sensitiveWords(List.of())
                .categories(List.of())
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
