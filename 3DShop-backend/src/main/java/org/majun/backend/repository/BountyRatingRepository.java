package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.BountyRating;

/**
 * 悬赏评价Mapper
 */
@Mapper
public interface BountyRatingRepository extends BaseMapper<BountyRating> {
}
