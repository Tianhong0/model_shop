package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户注销申请实体类
 */
@Data
@TableName("user_deletion_request")
@Schema(description = "用户注销申请")
public class UserDeletionRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("user_id")
    @Schema(description = "申请用户ID")
    private Long userId;

    @TableField("username")
    @Schema(description = "用户名")
    private String username;

    @TableField("phone")
    @Schema(description = "电话")
    private String phone;

    @TableField("reason")
    @Schema(description = "注销原因")
    private String reason;

    @TableField("status")
    @Schema(description = "状态: PENDING/APPROVED/REJECTED")
    private String status;

    @TableField("request_time")
    @Schema(description = "申请时间")
    private LocalDateTime requestTime;

    @TableField("approval_time")
    @Schema(description = "审批时间")
    private LocalDateTime approvalTime;

    @TableField("admin_note")
    @Schema(description = "管理员备注")
    private String adminNote;
}
