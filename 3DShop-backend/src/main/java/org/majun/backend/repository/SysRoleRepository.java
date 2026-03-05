package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.SysRole;

/**
 * 系统角色Mapper接口
 */
@Mapper
public interface SysRoleRepository extends BaseMapper<SysRole> {

}
