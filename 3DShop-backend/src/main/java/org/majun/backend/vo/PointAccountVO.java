package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "积分账户概览")
public class PointAccountVO {

    private String userId;

    private Integer availablePoints;

    private Integer totalEarned;

    private Integer totalSpent;

    private Integer status;
}
