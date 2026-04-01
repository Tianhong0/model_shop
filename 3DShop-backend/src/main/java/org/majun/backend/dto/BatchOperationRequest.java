package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 通用批量操作请求基类
 */
@Data
@Schema(description = "通用批量操作请求")
public class BatchOperationRequest {

    @NotEmpty(message = "ID列表不能为空")
    @Schema(description = "操作目标ID列表")
    private List<Long> ids;

    @Schema(description = "操作备注")
    private String remark;
}
