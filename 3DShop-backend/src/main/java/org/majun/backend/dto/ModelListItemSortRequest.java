package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "清单项排序请求")
public class ModelListItemSortRequest {

    @NotNull(message = "清单ID不能为空")
    @Schema(description = "清单ID")
    private Long listId;

    @NotEmpty(message = "排序列表不能为空")
    @Schema(description = "排序列表")
    private List<SortEntry> items;

    @Data
    @Schema(description = "排序条目")
    public static class SortEntry {

        @Schema(description = "清单项ID")
        private Long id;

        @Schema(description = "排序号")
        private Integer sortNo;
    }
}
