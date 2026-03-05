package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "后台运营状态")
public class AdminOperationStatusVO {

    @Schema(description = "是否营业中")
    private Boolean operating;

    @Schema(description = "状态更新时间")
    private LocalDateTime updatedAt;
}
