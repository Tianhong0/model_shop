package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Order comment list item")
public class OrderCommentListVO {

    @Schema(description = "Comment ID")
    private Long id;

    @Schema(description = "Order ID")
    private Long orderId;

    @Schema(description = "Model ID")
    private Long modelId;

    @Schema(description = "Model name")
    private String modelName;

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "User nickname")
    private String userNickname;

    @Schema(description = "User avatar")
    private String userAvatar;

    @Schema(description = "Model score")
    private Integer modelScore;

    @Schema(description = "Print score")
    private Integer printScore;

    @Schema(description = "Service score")
    private Integer serviceScore;

    @Schema(description = "Average score")
    private Double avgScore;

    @Schema(description = "Comment text")
    private String commentText;

    @Schema(description = "Comment images, comma separated")
    private String commentImages;

    @Schema(description = "Is anonymous")
    private Integer isAnonymous;

    @Schema(description = "Reply content")
    private String replyContent;

    @Schema(description = "Reply time")
    private LocalDateTime replyTime;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "追评数")
    private Integer replyCount;

    @Schema(description = "Status")
    private Integer status;

    @Schema(description = "Create time")
    private LocalDateTime createTime;
}
