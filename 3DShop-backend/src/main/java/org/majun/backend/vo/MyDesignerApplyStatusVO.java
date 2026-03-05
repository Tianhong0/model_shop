package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "当前用户设计者申请状态")
public class MyDesignerApplyStatusVO {

    @Schema(description = "是否已具备设计者角色")
    private Boolean alreadyDesigner;

    @Schema(description = "最新申请记录")
    private DesignerApplyRequestVO latestApply;

    @Schema(description = "服务器时间")
    private LocalDateTime serverTime;
}
