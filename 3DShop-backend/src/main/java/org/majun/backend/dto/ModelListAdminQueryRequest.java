package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "模型清单管理端分页查询请求")
public class ModelListAdminQueryRequest {

    @Schema(description = "页码", defaultValue = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页数量", defaultValue = "10")
    private Integer pageSize = 10;

    @Schema(description = "标题关键词")
    private String keyword;

    @Schema(description = "状态筛选: 0-草稿, 1-已发布, 2-已下架")
    private Integer status;

    @Schema(description = "创建者ID")
    private Long userId;

    @Schema(description = "创建时间开始")
    private LocalDateTime createTimeStart;

    @Schema(description = "创建时间结束")
    private LocalDateTime createTimeEnd;

    @Schema(description = "排序: latest/createTime/viewCount/likeCount")
    private String orderBy = "latest";
}
