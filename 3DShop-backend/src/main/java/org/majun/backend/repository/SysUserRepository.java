package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.SysUser;

/**
 * 系统用户Mapper接口
 */
@Mapper
public interface SysUserRepository extends BaseMapper<SysUser> {

}
