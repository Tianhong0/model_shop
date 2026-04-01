package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量操作结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "批量操作结果")
public class BatchOperationResultVO {

    @Schema(description = "总数量")
    private Integer total;

    @Schema(description = "成功数量")
    private Integer successCount;

    @Schema(description = "失败数量")
    private Integer failCount;

    @Schema(description = "失败详情列表")
    @Builder.Default
    private List<FailureDetail> failures = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "失败详情")
    public static class FailureDetail {
        @Schema(description = "目标ID")
        private Long id;

        @Schema(description = "失败原因")
        private String reason;
    }

    public static BatchOperationResultVO success(int total) {
        return BatchOperationResultVO.builder()
                .total(total)
                .successCount(total)
                .failCount(0)
                .failures(new ArrayList<>())
                .build();
    }

    public static BatchOperationResultVO partial(int total, int success, List<FailureDetail> failures) {
        return BatchOperationResultVO.builder()
                .total(total)
                .successCount(success)
                .failCount(total - success)
                .failures(failures != null ? failures : new ArrayList<>())
                .build();
    }
}
