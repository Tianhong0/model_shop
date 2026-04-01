package org.majun.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.majun.backend.entity.SysOperationLog;

/**
 * 操作日志Mapper接口
 */
@Mapper
public interface SysOperationLogRepository extends BaseMapper<SysOperationLog> {

}
