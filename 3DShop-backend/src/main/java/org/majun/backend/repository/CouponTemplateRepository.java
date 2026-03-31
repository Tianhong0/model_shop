package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.CouponTemplate;

/**
 * 优惠券模板 Mapper 接口
 */
@Mapper
public interface CouponTemplateRepository extends BaseMapper<CouponTemplate> {
}
