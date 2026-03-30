package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.SysEventSubmission;

/**
 * 活动作品提交Mapper
 */
@Mapper
public interface SysEventSubmissionRepository extends BaseMapper<SysEventSubmission> {
}
