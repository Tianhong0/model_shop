package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "模型收藏切换结果")
public class ModelFavoriteToggleVO {

    @Schema(description = "模型ID")
    private Long modelId;

    @Schema(description = "切换后是否收藏")
    private Boolean active;
}
