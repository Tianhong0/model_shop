package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 悬赏评价展示VO
 */
@Data
@Schema(description = "悬赏评价信息")
public class BountyRatingVO {

    @Schema(description = "评价ID")
    private Long id;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "设计者ID")
    private Long designerId;

    @Schema(description = "评分1-5星")
    private Integer score;

    @Schema(description = "评价内容")
    private String comment;

    @Schema(description = "评价图片列表")
    private List<String> images;

    @Schema(description = "是否匿名")
    private Integer isAnonymous;

    @Schema(description = "发布者名称")
    private String publisherName;

    @Schema(description = "设计者名称")
    private String designerName;

    @Schema(description = "任务标题")
    private String taskTitle;

    @Schema(description = "评价状态: 1正常, 0无效")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
