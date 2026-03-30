package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.SysEventParticipation;

/**
 * 用户活动参与Mapper
 */
@Mapper
public interface SysEventParticipationRepository extends BaseMapper<SysEventParticipation> {
}
