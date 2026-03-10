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
@TableName("used_message")
public class UsedMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("listing_id")
    private Long listingId;

    @TableField("room_key")
    private String roomKey;

    @TableField("sender_id")
    private Long senderId;

    @TableField("sender_role")
    private String senderRole;

    @TableField("counterpart_id")
    private Long counterpartId;

    @TableField("message_type")
    private Integer messageType;

    @TableField("content")
    private String content;

    @TableField("attachments")
    private String attachments;

    @TableField("is_system")
    private Integer isSystem;

    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
