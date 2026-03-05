package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 设计者列表VO
 */
@Data
@Schema(description = "设计者列表VO")
public class DesignerVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "登录账户")
    private String userName;

    @Schema(description = "显示名称/昵称")
    private String nickname;

    @Schema(description = "用户头像")
    private String avatar;

    @Schema(description = "模型ID列表（设计者专属）")
    private List<Long> modelIds;
}