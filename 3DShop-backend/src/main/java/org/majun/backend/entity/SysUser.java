package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户实体类
 */
@Data
@TableName("sys_user")
@Schema(description = "系统用户")
public class SysUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 登录账户
     */
    @TableField("user_name")
    @Schema(description = "登录账户")
    private String userName;

    /**
     * 账户密码(BCrypt加密)
     */
    @TableField("user_pwd")
    @Schema(description = "账户密码(BCrypt加密)")
    private String userPwd;

    /**
     * 显示名称/昵称
     */
    @TableField("nickname")
    @Schema(description = "显示名称/昵称")
    private String nickname;

    /**
     * 性别：1=男, 0=女
     */
    @TableField("sex")
    @Schema(description = "性别：1=男, 0=女")
    private Integer sex;

    /**
     * 电话
     */
    @TableField("mobile")
    @Schema(description = "电话")
    private String mobile;

    /**
     * 邮箱
     */
    @TableField("email")
    @Schema(description = "邮箱")
    private String email;

    /**
     * 状态：1-正常, 0-禁用
     */
    @TableField("status")
    @Schema(description = "状态：1-正常, 0-禁用")
    private Integer status;

    /**
     * 逻辑删除：1-已删, 0-未删
     */
    @TableField("is_delete")
    @TableLogic
    @Schema(description = "逻辑删除：1-已删, 0-未删")
    private Integer isDelete;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 用户头像
     */
    @TableField("avatar")
    @Schema(description = "用户头像")
    private String avatar;

    /**
     * 邀请码ID
     */
    @TableField("invite_code_id")
    @Schema(description = "邀请码ID")
    private Long inviteCodeId;

    /**
     * 邀请人ID
     */
    @TableField("inviter_id")
    @Schema(description = "邀请人ID")
    private Long inviterId;
}
