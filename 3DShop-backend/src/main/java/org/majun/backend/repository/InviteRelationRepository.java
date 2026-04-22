package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.InviteRelation;

@Mapper
public interface InviteRelationRepository extends BaseMapper<InviteRelation> {
}
