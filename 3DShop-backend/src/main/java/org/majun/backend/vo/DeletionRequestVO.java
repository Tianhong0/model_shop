package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 注销申请响应VO
 */
@Data
@Schema(description = "注销申请信息")
public class DeletionRequestVO {

    @Schema(description = "申请ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "电话")
    private String phone;

    @Schema(description = "注销原因")
    private String reason;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "申请时间")
    private LocalDateTime requestTime;

    @Schema(description = "审批时间")
    private LocalDateTime approvalTime;

    @Schema(description = "管理员备注")
    private String adminNote;
}
