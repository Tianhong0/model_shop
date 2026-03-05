package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "帖子列表项")
public class PostListVO {

    @Schema(description = "帖子ID")
    private Long id;

    @Schema(description = "发布者ID")
    private Long userId;

    @Schema(description = "发布者昵称")
    private String userNickname;

    @Schema(description = "发布者头像")
    private String userAvatar;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名")
    private String categoryName;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "收藏数")
    private Integer collectCount;

    @Schema(description = "回复数")
    private Integer replyCount;

    @Schema(description = "是否置顶")
    private Integer isTop;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "当前用户是否已点赞")
    private Boolean liked;

    @Schema(description = "当前用户是否已收藏")
    private Boolean collected;

    @Schema(description = "媒体列表")
    private List<PostMediaVO> mediaList;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
