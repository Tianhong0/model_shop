package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.SysRolePermission;

/**
 * 角色权限关联Mapper接口
 */
@Mapper
public interface SysRolePermissionRepository extends BaseMapper<SysRolePermission> {

}
