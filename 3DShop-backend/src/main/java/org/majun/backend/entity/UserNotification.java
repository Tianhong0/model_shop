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
@TableName("user_notification")
public class UserNotification implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("category")
    private String category;

    @TableField("notification_type")
    private String notificationType;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("cover_url")
    private String coverUrl;

    @TableField("sender_id")
    private Long senderId;

    @TableField("sender_name")
    private String senderName;

    @TableField("biz_id")
    private Long bizId;

    @TableField("biz_no")
    private String bizNo;

    @TableField("redirect_url")
    private String redirectUrl;

    @TableField("popup_required")
    private Integer popupRequired;

    @TableField("popup_pushed")
    private Integer popupPushed;

    @TableField("is_read")
    private Integer isRead;

    @TableField("ext_json")
    private String extJson;

    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}