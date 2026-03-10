package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.UsedAfterSale;

@Mapper
public interface UsedAfterSaleRepository extends BaseMapper<UsedAfterSale> {
}
