package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 作品评论VO
 */
@Data
@Schema(description = "作品评论VO")
public class SubmissionCommentVO {

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "评论用户ID")
    private Long userId;

    @Schema(description = "评论用户昵称")
    private String userName;

    @Schema(description = "评论用户头像")
    private String userAvatar;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "当前用户是否已点赞")
    private Boolean isLiked;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "父评论ID")
    private Long parentId;

    @Schema(description = "回复的用户昵称")
    private String replyToUserName;

    @Schema(description = "子评论列表")
    private List<SubmissionCommentVO> children;
}
