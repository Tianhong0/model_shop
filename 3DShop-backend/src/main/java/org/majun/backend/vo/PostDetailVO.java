package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "帖子详情")
public class PostDetailVO {

    @Schema(description = "帖子基础信息")
    private PostListVO post;

    @Schema(description = "帖子正文")
    private String content;

    @Schema(description = "回复列表")
    private List<PostReplyVO> replies;
}
