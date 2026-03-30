package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 活动查询请求
 */
@Data
@Schema(description = "活动查询请求")
public class EventQueryRequest {

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;

    @Schema(description = "页码(兼容)")
    private Integer page;

    @Schema(description = "每页大小(兼容)")
    private Integer size;

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "活动类型: 1-设计竞赛, 2-线下活动, 3-其他")
    private Integer eventType;

    @Schema(description = "状态: upcoming-即将开始, ongoing-进行中, ended-已结束")
    private String status;

    @Schema(description = "活动ID(用于作品查询)")
    private Long eventId;
}
