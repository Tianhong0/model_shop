package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.UsedReport;

@Mapper
public interface UsedReportRepository extends BaseMapper<UsedReport> {
}
