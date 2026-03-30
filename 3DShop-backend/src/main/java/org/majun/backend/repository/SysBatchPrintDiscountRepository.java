package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.SysBatchPrintDiscount;

/**
 * 批量打印折扣配置Mapper
 */
@Mapper
public interface SysBatchPrintDiscountRepository extends BaseMapper<SysBatchPrintDiscount> {

}
