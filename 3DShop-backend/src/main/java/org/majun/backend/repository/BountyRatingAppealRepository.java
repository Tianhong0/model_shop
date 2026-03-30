package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.BountyRatingAppeal;

/**
 * 评价申诉Mapper
 */
@Mapper
public interface BountyRatingAppealRepository extends BaseMapper<BountyRatingAppeal> {
}
