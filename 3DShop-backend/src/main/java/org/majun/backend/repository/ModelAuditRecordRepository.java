package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.ModelAuditRecord;

@Mapper
public interface ModelAuditRecordRepository extends BaseMapper<ModelAuditRecord> {
}
