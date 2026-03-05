package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "帖子回复")
public class PostReplyVO {

    @Schema(description = "回复ID")
    private Long id;

    @Schema(description = "帖子ID")
    private Long postId;

    @Schema(description = "回复用户ID")
    private Long userId;

    @Schema(description = "回复昵称")
    private String userNickname;

    @Schema(description = "回复头像")
    private String userAvatar;

    @Schema(description = "父回复ID")
    private Long parentId;

    @Schema(description = "回复内容")
    private String content;

    @Schema(description = "是否采纳")
    private Integer isAdopted;

    @Schema(description = "是否优质")
    private Integer isExcellent;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "当前用户是否点赞")
    private Boolean liked;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
