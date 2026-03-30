package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "模型清单管理端视图")
public class ModelListAdminVO {

    @Schema(description = "清单ID")
    private Long id;

    @Schema(description = "创建者ID")
    private Long userId;

    @Schema(description = "创建者昵称")
    private String userNickname;

    @Schema(description = "创建者头像")
    private String userAvatar;

    @Schema(description = "清单标题")
    private String title;

    @Schema(description = "清单描述")
    private String description;

    @Schema(description = "封面图URL")
    private String coverImage;

    @Schema(description = "包含模型数量")
    private Integer modelCount;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "收藏数")
    private Integer collectCount;

    @Schema(description = "状态: 0-草稿, 1-已发布, 2-已下架")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
