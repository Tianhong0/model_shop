package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "普通用户申请成为设计者请求")
public class DesignerApplySubmitRequest {

    @NotBlank(message = "申请理由不能为空")
    @Size(max = 1000, message = "申请理由长度不能超过1000个字符")
    private String applyReason;

    @Schema(description = "附件URL列表，逗号分隔（模型/图片/视频）")
    @Size(max = 4000, message = "附件URL总长度不能超过4000个字符")
    private String attachmentUrls;
}
