package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "模型清单详情")
public class ModelListShareDetailVO {

    @Schema(description = "清单基本信息")
    private ModelListShareListVO listInfo;

    @Schema(description = "清单包含的模型列表")
    private List<ModelListShareItemVO> items;
}
