package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户查询请求
 */
@Data
@Schema(description = "用户查询请求")
public class UserQueryRequest {

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "状态：1-正常，0-禁用")
    private Integer status;

    @Schema(description = "性别：1-男，0-女")
    private Integer sex;

    @Schema(description = "角色ID（筛选指定角色的用户）")
    private Long roleId;

    @Schema(description = "是否管理员（快捷筛选，true-仅管理员）")
    private Boolean isAdmin;

    @Schema(description = "页码")
    private Integer page = 1;

    @Schema(description = "每页大小")
    private Integer size = 10;
}