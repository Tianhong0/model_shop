package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "模型清单交互切换结果")
public class ModelListInteractionToggleVO {

    @Schema(description = "清单ID")
    private Long listId;

    @Schema(description = "交互类型: 1点赞/2收藏")
    private Integer interactType;

    @Schema(description = "切换后是否生效")
    private Boolean active;
}
