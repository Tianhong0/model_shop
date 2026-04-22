package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 推广分享记录实体类
 */
@Data
@TableName("promotion_share")
@Schema(description = "推广分享记录")
public class PromotionShare implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("user_id")
    @Schema(description = "分享用户ID")
    private Long userId;

    @TableField("share_type")
    @Schema(description = "分享类型")
    private String shareType;

    @TableField("share_channel")
    @Schema(description = "分享渠道")
    private String shareChannel;

    @TableField("ref_type")
    @Schema(description = "关联类型")
    private String refType;

    @TableField("ref_id")
    @Schema(description = "关联ID")
    private Long refId;

    @TableField("share_url")
    @Schema(description = "分享链接")
    private String shareUrl;

    @TableField("poster_url")
    @Schema(description = "海报图片URL")
    private String posterUrl;

    @TableField("click_count")
    @Schema(description = "点击次数")
    private Integer clickCount;

    @TableField("convert_count")
    @Schema(description = "转化次数")
    private Integer convertCount;

    @TableField("is_delete")
    @TableLogic
    @Schema(description = "逻辑删除")
    private Integer isDelete;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
