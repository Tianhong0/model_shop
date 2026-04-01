package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理员统计VO
 */
@Data
@Schema(description = "管理员统计VO")
public class AdminStatsVO {

    @Schema(description = "管理员总数")
    private Long totalAdmins;

    @Schema(description = "启用中的管理员数")
    private Long activeAdmins;

    @Schema(description = "禁用的管理员数")
    private Long disabledAdmins;

    @Schema(description = "近7日登录数")
    private Long weeklyLoginCount;
}
