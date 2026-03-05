package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.SysMaterial;

/**
 * 材质Mapper接口
 */
@Mapper
public interface SysMaterialRepository extends BaseMapper<SysMaterial> {

}
