package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("cs_conversation")
public class CsConversation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("session_no")
    private String sessionNo;

    @TableField("user_id")
    private Long userId;

    @TableField("user_nickname")
    private String userNickname;

    @TableField("user_avatar")
    private String userAvatar;

    @TableField("admin_id")
    private Long adminId;

    @TableField("admin_nickname")
    private String adminNickname;

    @TableField("status")
    private Integer status;

    @TableField("end_reason")
    private String endReason;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("last_message_time")
    private LocalDateTime lastMessageTime;

    @TableField("unread_user_count")
    private Integer unreadUserCount;

    @TableField("unread_admin_count")
    private Integer unreadAdminCount;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
