package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.UserCoupon;

/**
 * 用户优惠券 Mapper 接口
 */
@Mapper
public interface UserCouponRepository extends BaseMapper<UserCoupon> {
}
