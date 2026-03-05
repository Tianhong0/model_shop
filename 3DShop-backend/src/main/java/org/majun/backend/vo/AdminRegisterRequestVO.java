package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "后台管理员注册申请信息")
public class AdminRegisterRequestVO {

    private Long id;

    private String userName;

    private String nickname;

    private String mobile;

    private String email;

    private String status;

    private LocalDateTime requestTime;

    private LocalDateTime reviewTime;

    private Long reviewBy;

    private String reviewRemark;

    private LocalDateTime retryAfter;

    private Long approvedUserId;
}
