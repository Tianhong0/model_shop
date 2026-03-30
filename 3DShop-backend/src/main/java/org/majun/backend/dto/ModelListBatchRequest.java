package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "模型清单批量操作请求")
public class ModelListBatchRequest {

    @NotEmpty(message = "清单ID列表不能为空")
    @Schema(description = "清单ID列表")
    private List<Long> listIds;

    @Schema(description = "批量状态操作时使用: 1-发布, 2-下架")
    private Integer status;
}
