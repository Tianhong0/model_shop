package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "打印任务事件")
public class PrintJobEventVO {
    private Long id;
    private Long jobId;
    private String eventType;
    private String eventMessage;
    private String eventPayload;
    private LocalDateTime createTime;
}
