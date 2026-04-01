package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 批量审核请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "批量审核请求")
public class BatchReviewRequest extends BatchOperationRequest {

    @NotNull(message = "审核状态不能为空")
    @Schema(description = "审核状态: APPROVED-通过, REJECTED-拒绝")
    private String reviewStatus;

    @Schema(description = "审核意见")
    private String reviewRemark;

    /**
     * 快速创建通过请求
     */
    public static BatchReviewRequest approve(java.util.List<Long> ids, String remark) {
        BatchReviewRequest request = new BatchReviewRequest();
        request.setIds(ids);
        request.setReviewStatus("APPROVED");
        request.setReviewRemark(remark);
        return request;
    }

    /**
     * 快速创建拒绝请求
     */
    public static BatchReviewRequest reject(java.util.List<Long> ids, String reason) {
        BatchReviewRequest request = new BatchReviewRequest();
        request.setIds(ids);
        request.setReviewStatus("REJECTED");
        request.setReviewRemark(reason);
        return request;
    }
}
