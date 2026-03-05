package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.SysOrder;

/**
 * Order mapper.
 */
@Mapper
public interface SysOrderRepository extends BaseMapper<SysOrder> {

}
