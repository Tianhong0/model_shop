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
@TableName("designer_apply_request")
@Schema(description = "普通用户申请成为设计者")
public class DesignerApplyRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    @TableField("nickname")
    private String nickname;

    @TableField("mobile")
    private String mobile;

    @TableField("email")
    private String email;

    @TableField("apply_reason")
    private String applyReason;

    @TableField("attachment_urls")
    private String attachmentUrls;

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
}
