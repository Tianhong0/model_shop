package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.SysConfig;

/**
 * 系统配置Mapper接口
 */
@Mapper
public interface SysConfigRepository extends BaseMapper<SysConfig> {

}
