package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.SysModel;

/**
 * 模型Mapper接口
 */
@Mapper
public interface SysModelRepository extends BaseMapper<SysModel> {

}
