package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.SysBanner;

/**
 * 轮播图Mapper
 */
@Mapper
public interface SysBannerRepository extends BaseMapper<SysBanner> {
}
