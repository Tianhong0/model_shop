package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("cs_admin_status")
public class CsAdminStatus implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("admin_id")
    private Long adminId;

    @TableField("admin_nickname")
    private String adminNickname;

    @TableField("is_online")
    private Integer isOnline;

    @TableField("current_conversation_count")
    private Integer currentConversationCount;

    @TableField("total_served_count")
    private Integer totalServedCount;

    @TableField("last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
