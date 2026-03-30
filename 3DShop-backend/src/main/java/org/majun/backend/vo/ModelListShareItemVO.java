package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "清单内模型项")
public class ModelListShareItemVO {

    @Schema(description = "清单项ID")
    private Long id;

    @Schema(description = "模型ID")
    private Long modelId;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "主图URL")
    private String mainImageUrl;

    @Schema(description = "设计者昵称")
    private String designerName;

    @Schema(description = "基础价格")
    private BigDecimal basePrice;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "推荐理由")
    private String remark;

    @Schema(description = "排序号")
    private Integer sortNo;
}
