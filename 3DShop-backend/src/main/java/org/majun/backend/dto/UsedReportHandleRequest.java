package org.majun.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UsedReportHandleRequest {

    @NotNull(message = "举报ID不能为空")
    private Long reportId;

    @NotNull(message = "处理结论不能为空")
    private Boolean approved;

    private String handleAction;

    private String remark;
}
