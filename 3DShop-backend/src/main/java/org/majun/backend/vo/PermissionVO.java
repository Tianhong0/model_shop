package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限VO
 */
@Data
@Schema(description = "权限信息")
public class PermissionVO {

    @Schema(description = "权限ID")
    private Long id;

    @Schema(description = "权限标识")
    private String permissionCode;

    @Schema(description = "权限名称")
    private String permissionName;

    @Schema(description = "权限类型：MENU/BUTTON/API")
    private String permissionType;

    @Schema(description = "父级ID")
    private Long parentId;

    @Schema(description = "菜单路径")
    private String menuPath;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "子权限")
    private List<PermissionVO> children;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
