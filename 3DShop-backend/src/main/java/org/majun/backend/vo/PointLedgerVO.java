package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "积分流水")
public class PointLedgerVO {

    private String id;

    private Integer direction;

    private String bizType;

    private String bizNo;

    private String refId;

    private Integer points;

    private Integer beforePoints;

    private Integer afterPoints;

    private String remark;

    private LocalDateTime createTime;
}
