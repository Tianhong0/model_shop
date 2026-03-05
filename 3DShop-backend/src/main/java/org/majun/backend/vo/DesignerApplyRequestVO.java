package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "设计者申请信息")
public class DesignerApplyRequestVO {

    private Long id;

    private Long userId;

    private String userName;

    private String nickname;

    private String mobile;

    private String email;

    private String applyReason;

    private String attachmentUrls;

    private String status;

    private LocalDateTime requestTime;

    private LocalDateTime reviewTime;

    private Long reviewBy;

    private String reviewRemark;

    private LocalDateTime retryAfter;
}
