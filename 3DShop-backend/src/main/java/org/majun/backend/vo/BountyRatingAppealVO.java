package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价申诉展示VO
 */
@Data
@Schema(description = "评价申诉信息")
public class BountyRatingAppealVO {

    @Schema(description = "申诉ID")
    private Long id;

    @Schema(description = "评价ID")
    private Long ratingId;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "任务标题")
    private String taskTitle;

    @Schema(description = "设计者ID")
    private Long designerId;

    @Schema(description = "设计者名称")
    private String designerName;

    @Schema(description = "原评价分数")
    private Integer ratingScore;

    @Schema(description = "原评价内容")
    private String ratingComment;

    @Schema(description = "申诉理由")
    private String reason;

    @Schema(description = "证据材料URL列表")
    private List<String> evidence;

    @Schema(description = "状态: 0待处理, 1通过, 2驳回")
    private Integer status;

    @Schema(description = "管理员ID")
    private Long adminId;

    @Schema(description = "管理员备注")
    private String adminRemark;

    @Schema(description = "处理时间")
    private LocalDateTime processedTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
