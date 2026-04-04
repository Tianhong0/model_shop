package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 客服统计VO
 */
@Data
@Schema(description = "客服统计数据")
public class CsStatsVO {

    @Schema(description = "当前进行中的会话数")
    private Long activeConversations;

    @Schema(description = "累计服务会话数（已结束的会话）")
    private Long totalServed;
}
