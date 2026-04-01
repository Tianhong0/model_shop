package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户列表VO
 */
@Data
@Schema(description = "用户列表VO")
public class UserListVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "登录账户")
    private String userName;

    @Schema(description = "显示名称/昵称")
    private String nickname;

    @Schema(description = "性别：1=男, 0=女")
    private Integer sex;

    @Schema(description = "电话")
    private String mobile;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "状态：1-正常, 0-禁用")
    private Integer status;

    @Schema(description = "用户头像")
    private String avatar;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "角色ID列表")
    private List<Long> roleIds;

    @Schema(description = "角色名称列表")
    private List<String> roleNames;

    @Schema(description = "是否管理员")
    private Boolean isAdmin;

    @Schema(description = "模型ID列表（设计者专属）")
    private List<Long> modelIds;
}