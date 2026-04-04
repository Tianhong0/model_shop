package org.majun.backend.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 意图识别结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentResult {

    /**
     * 识别的意图类型
     */
    private IntentType intent;

    /**
     * 置信度 (0.0-1.0)
     */
    private Double confidence;

    /**
     * 判断原因
     */
    private String reason;
}
