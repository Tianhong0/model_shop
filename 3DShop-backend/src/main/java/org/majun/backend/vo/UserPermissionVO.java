package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 用户权限信息VO
 */
@Data
@Schema(description = "用户权限信息")
public class UserPermissionVO {

    /**
     * 权限编码列表
     */
    @Schema(description = "权限编码列表")
    private List<String> permissions;

    /**
     * 菜单树
     */
    @Schema(description = "菜单树")
    private List<PermissionVO> menus;
}
