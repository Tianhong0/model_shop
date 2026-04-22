package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户邀请码实体类
 */
@Data
@TableName("user_invite_code")
@Schema(description = "用户邀请码")
public class UserInviteCode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("user_id")
    @Schema(description = "用户ID")
    private Long userId;

    @TableField("invite_code")
    @Schema(description = "邀请码")
    private String inviteCode;

    @TableField("total_invited")
    @Schema(description = "累计邀请人数")
    private Integer totalInvited;

    @TableField("total_points_earned")
    @Schema(description = "累计获得积分")
    private Integer totalPointsEarned;

    @TableField("status")
    @Schema(description = "状态：1-正常, 0-禁用")
    private Integer status;

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
