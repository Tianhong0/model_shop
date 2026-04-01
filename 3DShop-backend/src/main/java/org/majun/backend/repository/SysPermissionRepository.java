package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.SysPermission;

/**
 * 系统权限Mapper接口
 */
@Mapper
public interface SysPermissionRepository extends BaseMapper<SysPermission> {

}
