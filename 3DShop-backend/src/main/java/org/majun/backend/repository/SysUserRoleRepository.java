package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.SysUserRole;

/**
 * 用户角色关联Mapper接口
 */
@Mapper
public interface SysUserRoleRepository extends BaseMapper<SysUserRole> {

}
