package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "模型清单统计数据")
public class ModelListStatisticsVO {

    @Schema(description = "总清单数")
    private Long totalCount;

    @Schema(description = "草稿数")
    private Long draftCount;

    @Schema(description = "已发布数")
    private Long publishedCount;

    @Schema(description = "已下架数")
    private Long offlineCount;

    @Schema(description = "总互动数(点赞+收藏)")
    private Long totalInteractions;

    @Schema(description = "总浏览量")
    private Long totalViews;

    @Schema(description = "创建最多清单的用户ID")
    private Long topCreatorId;

    @Schema(description = "创建最多清单的用户昵称")
    private String topCreatorNickname;

    @Schema(description = "该用户创建的清单数")
    private Long topCreatorCount;
}
