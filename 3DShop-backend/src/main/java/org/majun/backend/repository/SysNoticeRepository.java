package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.SysNotice;

/**
 * 公告Mapper
 */
@Mapper
public interface SysNoticeRepository extends BaseMapper<SysNotice> {
}
