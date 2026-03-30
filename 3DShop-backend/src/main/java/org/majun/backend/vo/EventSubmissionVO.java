package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 作品展示VO（不包含模型文件）
 */
@Data
@Schema(description = "作品展示VO")
public class EventSubmissionVO {

    @Schema(description = "作品ID")
    private Long id;

    @Schema(description = "活动ID")
    private Long eventId;

    @Schema(description = "作品标题")
    private String title;

    @Schema(description = "作品描述")
    private String description;

    @Schema(description = "作品图片URL列表")
    private List<String> imageUrls;

    @Schema(description = "模型文件URL列表（仅自己的作品可见）")
    private List<String> fileUrls;

    @Schema(description = "状态: 1-待审核, 2-已通过, 3-已拒绝")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "评分")
    private BigDecimal score;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "当前用户是否已点赞")
    private Boolean isLiked;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "作者ID")
    private Long authorId;

    @Schema(description = "作者昵称")
    private String authorName;

    @Schema(description = "作者头像")
    private String authorAvatar;

    @Schema(description = "是否为当前用户的作品")
    private Boolean isMine;
}
