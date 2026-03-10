package org.majun.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UsedReportCreateRequest {

    @NotBlank(message = "举报对象类型不能为空")
    private String targetType;

    @NotNull(message = "举报对象ID不能为空")
    private Long targetId;

    @NotBlank(message = "举报原因类型不能为空")
    private String reasonType;

    @NotBlank(message = "举报说明不能为空")
    private String reasonText;

    private String evidenceUrls;
}
