package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "后台仪表盘消息")
public class DashboardMessageVO {

    @Schema(description = "消息唯一键")
    private String key;

    @Schema(description = "标签")
    private String tag;

    @Schema(description = "标签类型(primary/success/warning/danger/info)")
    private String type;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "时间文案")
    private String time;

    @Schema(description = "点击跳转路由")
    private String route;

    @Schema(description = "是否未读")
    private Boolean unread;
}
