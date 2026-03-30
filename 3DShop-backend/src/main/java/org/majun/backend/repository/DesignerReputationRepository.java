package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.DesignerReputation;

/**
 * 设计者信誉Mapper
 */
@Mapper
public interface DesignerReputationRepository extends BaseMapper<DesignerReputation> {
}
