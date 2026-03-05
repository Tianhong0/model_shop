package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("admin_register_request")
@Schema(description = "后台管理员注册申请")
public class AdminRegisterRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_name")
    private String userName;

    @TableField("nickname")
    private String nickname;

    @TableField("mobile")
    private String mobile;

    @TableField("email")
    private String email;

    @TableField("password_hash")
    private String passwordHash;

    @TableField("status")
    private String status;

    @TableField("request_time")
    private LocalDateTime requestTime;

    @TableField("review_time")
    private LocalDateTime reviewTime;

    @TableField("review_by")
    private Long reviewBy;

    @TableField("review_remark")
    private String reviewRemark;

    @TableField("retry_after")
    private LocalDateTime retryAfter;

    @TableField("approved_user_id")
    private Long approvedUserId;
}
