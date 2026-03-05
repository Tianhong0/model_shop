package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "悬赏任务创建请求")
public class BountyTaskCreateRequest {

    @NotBlank(message = "标题不能为空")
    @Schema(description = "任务标题")
    private String title;

    @NotBlank(message = "需求描述不能为空")
    @Schema(description = "需求描述")
    private String description;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "标签，JSON数组字符串或逗号分隔")
    private String tags;

    @NotNull(message = "预算不能为空")
    @DecimalMin(value = "0.01", message = "预算金额必须大于0")
    @Schema(description = "预算金额")
    private BigDecimal budgetAmount;

    @Schema(description = "期望交付天数")
    private Integer expectedDays;

    @Schema(description = "截止时间，格式yyyy-MM-dd HH:mm:ss")
    private String deadlineTime;

    @Schema(description = "附件URL列表")
    private List<String> attachments;
}
