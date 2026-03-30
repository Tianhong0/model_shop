package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "创建模型清单请求")
public class ModelListCreateRequest {

    @NotBlank(message = "清单标题不能为空")
    @Schema(description = "清单标题")
    private String title;

    @Schema(description = "清单描述")
    private String description;

    @Schema(description = "封面图URL")
    private String coverImage;

    @Schema(description = "状态: 0-草稿, 1-发布")
    private Integer status = 0;

    @Valid
    @Schema(description = "初始模型列表")
    private List<ModelListItemEntry> items;

    @Data
    @Schema(description = "清单项条目")
    public static class ModelListItemEntry {

        @Schema(description = "模型ID")
        private Long modelId;

        @Schema(description = "推荐理由")
        private String remark;

        @Schema(description = "排序号")
        private Integer sortNo;
    }
}
